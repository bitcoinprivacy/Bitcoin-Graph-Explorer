// this has all the database stuff. to be extended as concrete DB implementations
package core

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{ StaticQuery => Q }
import scala.slick.jdbc.meta.MTable

import com.typesafe.config.ConfigFactory
import util._

trait BitcoinDB {
  def blockDB = TableQuery[Blocks]
  def addresses = TableQuery[Addresses]
  def movements = TableQuery[Movements]
  def richestAddresses = TableQuery[RichestAddresses]
  def richestClosures = TableQuery[RichestClosures]
  def stats = TableQuery[Stats]
  def utxo = TableQuery[UTXO]
  def balances = TableQuery[Balances]
  def closureBalances = TableQuery[ClosureBalances]

  def USERNAME = conf.getString("username")
  def PASSWORD = conf.getString("password")
  def HOST = conf.getString("host")
  def OPTIONS = conf.getString("jdbcOptions")
  def DBNAME = conf.getString("databaseName")
  def URL = "jdbc:postgresql://" + HOST + "/" + DBNAME + OPTIONS
  def DRIVER = "org.postgresql.Driver"

  def deleteIfExists(tables: TableQuery[_ <: Table[_]]*)(implicit session: Session) =
    tables foreach { table => if (!MTable.getTables(table.baseTableRow.tableName).list.isEmpty) table.ddl.drop }

  def transactionDBSession[X](f: => X): X =
    {
      Database.forURL(URL, user = USERNAME, password = PASSWORD, driver = DRIVER) withDynSession { f }
    }

  def countInputs: Int =
    transactionDBSession
  {
    movements.length.run
  }

  def blockCount: Int = transactionDBSession
  {
    blockDB.length.run
  }

  def existsOutput(transactionHash: Hash, index: Int): Boolean =
    {
      Q.queryNA[Int]("""
        select count(*) from movements where
        transaction_hash = """ + transactionHash + """ and
        `index` = """ + index).list.head > 0
    }

  def txListQuery(blocks: Seq[Int]) = {
    val emptyArray = Hash.zero(0).array.toArray
    transactionDBSession {
      for (q <- movements.filter(_.height_out inSet blocks).filter(_.address =!= emptyArray))
      yield (q.spent_in_transaction_hash, q.address)
      // in order to read quickly from db, we need to read in the order of insertion
    }
  }
  //  val txList = Compiled(txListQuery _) doesn't work with inSet


  def initializeReaderTables: Unit =
  {
    transactionDBSession{
      deleteIfExists(movements, blockDB, utxo)
      movements.ddl.create
      blockDB.ddl.create
      utxo.ddl.create
    }
  }

  def initializeClosureTables: Unit = {
    transactionDBSession{
      deleteIfExists(addresses)
      addresses.ddl.create
    }
  }

  def initializeStatsTables: Unit = {
    transactionDBSession{
      deleteIfExists(stats, richestAddresses, richestClosures)
      
      stats.ddl.create
      richestAddresses.ddl.create
      richestClosures.ddl.create
      
    }
  }

  def createBalanceTables = {
    var clock = System.currentTimeMillis
    transactionDBSession {
      deleteIfExists(balances, closureBalances)
      balances.ddl.create
      closureBalances.ddl.create

      Q.updateNA("insert into balances select address, sum(value) as balance from utxo group by address;").execute

      (Q.u + "create index addresses_balance on balances(address)").execute
        (Q.u + "create index balance on balances(balance)").execute

      Q.updateNA("insert into closure_balances select a.representant, sum(b.balance) as balance from balances b, addresses a where b.address = a.hash group by a.representant;").execute
      //Q.updateNA("insert into closure_balances select a.address, a.balance from balances a left outer join  addresses b on a.address = b.hash where b.representant is null").execute
      (Q.u + "create index addresses_balance_2 on closure_balances(representant)").execute
        (Q.u + "create index balance_2 on closure_balances(balance)").execute

      println("DONE: Balances updated in %s s" format (System.currentTimeMillis - clock)/1000)
    }
  }

  def insertRichestClosures = {
    println("DEBUG: Calculating richest closure list...")
    
    transactionDBSession {
      Q.updateNA( """
      insert
        into richest_closures
      select
        (select max(block_height) from blocks) as block_height,
        representant as address,
        balance
      from
        closure_balances
      order by
        balance desc
      limit 1000
      ;""").execute
    }
  }

  def insertRichestAddresses = {

    println("DEBUG: Calculating richest address list...")

    transactionDBSession {
      Q.updateNA( """
       insert
        into richest_addresses
       select
        (select max(block_height) from blocks) as block_height,
       address,
        balance
      from
        balances
      order by
        balance desc
      limit 1000
    ;""").execute
    }
  }

  def insertStatistics = {

    val (nonDustAddresses,addressGini) = getGini(balances)
    val (nonDustClosures, closureGini) = getGini(closureBalances)

    println("DEBUG: Calculating stats...")

    val startTime = System.currentTimeMillis
    transactionDBSession {
      val query =   """
       insert
        into stats select
        (select max(block_height) from blocks),
        (select sum(balance)/100000000 from balances),
        (select sum(txs) from blocks),
        
        (select count(1) from addresses),
        (select count(distinct(representant)) from addresses),
        (select count(1) from balances),
        (select count(1) from closure_balances),
        """ + nonDustAddresses + """,
        """ + nonDustClosures + """,
        """ + closureGini + """,
        """ + addressGini + """,
        """+ (System.currentTimeMillis/1000).toString +""";"""

      (Q.u + query).execute
      println("DONE: Stats calculated in " + (System.currentTimeMillis - startTime)/1000 + "s");

  }
}

  def getGini[A <: Table[_] with BalanceField](balanceTable: TableQuery[A]): (Long, Double) = {
    println("DEBUG: calculating Gini: " + balanceTable + java.util.Calendar.getInstance().getTime())
    val balanceVector = transactionDBSession {
       balanceTable.map(_.balance).filter(_ > dustLimit).sorted.run.toVector
    }
    val balances = balanceVector.map(_.toDouble)
    
    val n: Long = balances.length
    
    val summe = balances.sum
    val mainSum = balances.zipWithIndex.map(p => p._1*(p._2+1.0)/n).sum
    val gini:Double = 2.0*mainSum/(summe) - (n+1.0)/n
    (n, gini)
  }

  def createAddressIndexes {

    println("DEBUG: Creating indexes ...")
    val time = System.currentTimeMillis

    transactionDBSession {
      for (query <- List(
             "create index representant on addresses (representant)",
             "create unique index hash on addresses (hash)"
           ))
      {
        Q.updateNA(query).execute
        println("DEBUG: Finished "+ query)
      }


    }

    println("DONE: Indexes created in %s s" format (System.currentTimeMillis - time)/1000)

  }

  def createIndexes {

    println("DEBUG: Creating indexes ...")
    val time = System.currentTimeMillis

    transactionDBSession {

      // MOVEMENTS

      // get outputs from address
      for (query <- List(
             "create index address on movements (address);",
             """create unique index tx_idx  on movements (transaction_hash, "index");""",
             "create index  spent_in_transaction_hash2 on movements (spent_in_transaction_hash, address);",
             "create index height_in on movements (height_in);",
             "create index height_out_in on movements (height_out, height_in);",
             "create index address_utxo on utxo (address)",
             "create index height_utxo on utxo (block_height)",
             "create index tx_utxo on utxo (transaction_hash, index)",
              "create index  block_height on blocks(block_height);"))

      {
        Q.updateNA(query).execute
        println("DEBUG: Finished"+ query)
      }

    }

    println("DONE: Indexes created in %s s" format (System.currentTimeMillis - time)/1000)

  }

}
