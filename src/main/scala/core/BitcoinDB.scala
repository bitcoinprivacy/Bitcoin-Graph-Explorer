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

  def initializeBalanceTable: Unit = {
    transactionDBSession{
      deleteIfExists(balances, stats, richestAddresses, richestClosures, closureBalances)
      balances.ddl.create
      stats.ddl.create
      richestAddresses.ddl.create
      richestClosures.ddl.create
      closureBalances.ddl.create
    }
  }
}
