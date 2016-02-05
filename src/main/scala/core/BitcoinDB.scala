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
      deleteIfExists(stats, richestAddresses, richestClosures, balances, closureBalances)
      
      stats.ddl.create
      richestAddresses.ddl.create
      richestClosures.ddl.create
      balances.ddl.create
      closureBalances.ddl.create

    }
  }

  def createBalanceTables = {
    var clock = System.currentTimeMillis
    transactionDBSession {
      println("DEBUG: Creating balances")
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

      println("DONE: Balances created in %s s" format (System.currentTimeMillis - clock)/1000)
    }
  }

  def countUTXOs = {
    transactionDBSession {
      val values = (for (u <- utxo)
      yield
        u.value).run.toVector

      (values.size, values.sum)
    }
  }

  def updateBalanceTables(changedAddresses: collection.mutable.Map[Hash,Long]) = {
    var clock = System.currentTimeMillis
    println("DEBUG: Updating balances ...")
    currentStat.total_bitcoins_in_addresses+=changedAddresses.map{_._2}.sum.intValue

    transactionDBSession {

      val adsAndBalances = for ((address, change) <- changedAddresses)
                           yield (address,
                                  balances.filter(_.address === Hash.hashToArray(address)).
                                    map (_.balance).firstOption.getOrElse(0L) + change)
      for {
        (address, balance) <- adsAndBalances
        addressArray = Hash.hashToArray(address)
      }
      if (balance != 0L)
        balances.insertOrUpdate(addressArray, balance)
      else
        balances.filter(_.address === addressArray).delete

      val table = LmdbMap.open("closures")
      val unionFindTable = new ClosureMap(table)
      val closures = new DisjointSets[Hash](unionFindTable)

      val repsAndChanges: collection.mutable.Map[Hash,Long] = collection.mutable.Map()
      for ((address, balance) <- adsAndBalances -= Hash.zero(0))
      {
        val repOpt = closures.find(address)._1

        for (rep <- repOpt){
          val newBalance = repsAndChanges.getOrElse(rep, 0L) + balance
          repsAndChanges += (rep -> newBalance)
        }
      }

      val repsAndBalances = for ((rep, change) <- repsAndChanges)
                           yield (rep,
                                  closureBalances.filter(_.representant === Hash.hashToArray(rep)).
                                    map (_.balance).firstOption.getOrElse(0L) + change)
      for { // TODO: make code DRYer by unifiying this in a common function with the same functionality above for balances
        (address, balance) <- repsAndBalances
        addressArray = Hash.hashToArray(address)
      }
      if (balance != 0L)
        closureBalances.insertOrUpdate(addressArray, balance)
      else
        closureBalances.filter(_.representant === addressArray).delete

      table.close

      println("DONE: %s balances updated in %s s, %s µs per address "
                format
                (adsAndBalances.size, (System.currentTimeMillis - clock)/1000, (System.currentTimeMillis - clock)*1000/(adsAndBalances.size+1)))
    
    }
  }

  def insertRichestClosures = {
    println("DEBUG: Calculating richest closure list...")
    var startTime = System.currentTimeMillis
    transactionDBSession {
      val bh = blockCount-1
      val topClosures = closureBalances.sortBy(_.balance.desc).take(1000).run.toVector
      val topAddresses = richestAddresses.sortBy(_.block_height.desc).take(1000).run.toVector
      val repsAndBalances = topAddresses map { p =>  
        (addresses.filter(_.hash === p._2).map(_.representant).firstOption.getOrElse(p._2),p._3)
      }
      val topClosureReps = topClosures map (p => Hash(p._1)) //need Hash in order to compare
      val filtered = for {
        (rep,bal) <- repsAndBalances
        if (!topClosureReps.contains(Hash(rep)))
      } yield (rep, bal)

      val mixed = filtered ++ topClosures
      val mixedWithBh = for ((rep,bal) <- mixed) yield (bh,rep,bal)
      richestClosures.insertAll(mixedWithBh: _*)

      println("RichestList calculated in " + (System.currentTimeMillis - startTime)/1000 + "s")
    }
  }

  def insertRichestAddresses = {

    println("DEBUG: Calculating richest address list...")
    var startTime = System.currentTimeMillis

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
      where address!= ''
      order by
        balance desc
      limit 1000
    ;""").execute
      println("RichestList calculated in " + (System.currentTimeMillis - startTime)/1000 + "s")
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
       (select coalesce(max(block_height),0) from blocks),
       (select coalesce(sum(balance)/100000000,0) from balances),
       (select coalesce(sum(txs),0) from blocks),
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

  def updateStatistics = {

    println("Updating stats")
    val time = System.currentTimeMillis
    val (nonDustAddresses, addressGini) = getGini(balances)
    val (nonDustClosures, closureGini) = getGini(closureBalances)

    /* block_height 
       total_bitcoins_in_addresses XXX
       total_transactions YYY
       total_addresses XXX
       total_closures XXX
       total_addresses_with_balance YYY
       total_closures_with_balance YYY
       total_addresses_no_dust YYY
       total_closures_no_dust YYY
       gini_address 
       gini_closure 
       tstamp      */

    // Right now with update statistics we update the values with XXX in a faster way than reading all the database. We can improve it moving the rest (YYY) to a better position, or even using psql triggers.
    // A first approach could be to modify the values direct in ResumeBlockReader, ResumeClosure (balances can be updated whenever a utxo is added or removed)
    // Update should only add the ginis and call saveStat
    transactionDBSession{
      currentStat.total_addresses_with_balance+=balances.length.run
      currentStat.total_closures_with_balance+=closureBalances.length.run
      currentStat.total_addresses_no_dust+= nonDustAddresses.intValue
      currentStat.total_closures_no_dust+= nonDustClosures.intValue
      currentStat.gini_closure=closureGini
      currentStat.gini_address=addressGini
      currentStat.block_height=blockCount
      currentStat.tstamp=System.currentTimeMillis/1000
      currentStat.total_transactions = blockDB.map(_.txs).filter(_ > 0).sum.run.getOrElse(0)
      saveStat
      println("Updated in " + (System.currentTimeMillis - time)/1000 + " seconds")
    }
  }

  def getGini[A <: Table[_] with BalanceField](balanceTable: TableQuery[A]): (Long, Double) = {
    println("DEBUG: calculating Gini: " + balanceTable + java.util.Calendar.getInstance().getTime())
    val time = System.currentTimeMillis

    val balanceVector = transactionDBSession {
       balanceTable.map(_.balance).filter(_ > dustLimit).sorted.run.toVector
    }

    val balances = balanceVector.map(_.toDouble)

    val n: Long = balances.length

    val summe = balances.sum
    val mainSum = balances.zipWithIndex.map(p => p._1*(p._2+1.0)/n).sum
    val gini:Double = if (n==0) 0.0 else 2.0*mainSum/(summe) - (n+1.0)/n
    println("DONE: gini calculated in " + (System.currentTimeMillis - time)/1000 + "s")
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

  def createIndexes = {

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
              "create index block_height on blocks(block_height);"))

      {
        Q.updateNA(query).execute
        println("DEBUG: Finished"+ query)
      }

    }

    println("DONE: Indexes created in %s s" format (System.currentTimeMillis - time)/1000)

  }

  lazy val currentStat = transactionDBSession{
    CurrentStat.tupled(stats.sortBy(_.block_height desc).firstOption.get)
  }

  def saveStat = transactionDBSession{
    stats.insert(CurrentStat.unapply(currentStat).get)
  }

  case class CurrentStat (var block_height: Int, var total_bitcoins_in_addresses: Int, var total_transactions: Int, var total_addresses: Int, var total_closures: Int, var total_addresses_with_balance: Int,
                          var total_closures_with_balance: Int, var total_addresses_no_dust: Int, var total_closures_no_dust: Int, var gini_closure: Double, var gini_address: Double, var tstamp: Long)
  
  def lastCompletedHeight: Int = transactionDBSession{
    stats.map(_.block_height).max.run.getOrElse(0)
  }

  def rollBack = transactionDBSession {

    val blockHeight = blockCount - 1
    stats.filter(_.block_height === blockHeight).delete
    richestAddresses.filter(_.block_height === blockHeight).delete
    richestClosures.filter(_.block_height === blockHeight).delete
    val table = LmdbMap.open("utxos")
    val utxoTable = new UTXOs(table)

    val utxoQuery = utxo.filter(_.block_height === blockHeight)

    for ((tx,idx) <- utxoQuery.map(p => (p.transaction_hash,p.index)).run)
      utxoTable -= Hash(tx) -> idx
    utxoQuery.delete

    val movementQuery = movements.filter(_.height_out === blockHeight)
    val utxoRows = movementQuery.filter(_.height_in =!= blockHeight).map(p => (p.transaction_hash,p.address, p.index, p.value, p.height_in)).run
    for ((tx,ad,idx,v,h) <- utxoRows)
      utxoTable += ((Hash(tx) -> idx) -> (Hash(ad),v,h))
    utxo.insertAll(utxoRows:_*)   

    blockDB.filter(_.block_height === blockHeight).delete

    table.close
  }
}
