// this has all the database stuff. to be extended as concrete DB implementations
package core

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{ StaticQuery => Q }
import scala.slick.jdbc.meta.MTable

import com.typesafe.config.ConfigFactory
import util._

trait BitcoinDB {
  val blockDB = TableQuery[Blocks]
  val addresses = TableQuery[Addresses]
  val movements = TableQuery[Movements]
  val richestAddresses = TableQuery[RichestAddresses]
  val richestClosures = TableQuery[RichestClosures]
  val stats = TableQuery[Stats]
  val utxo = TableQuery[UTXO]

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

  def existsOutput(transactionHash: Hash, index: Int): Boolean =
    {
      Q.queryNA[Int]("""
        select count(*) from movements where
        transaction_hash = """ + transactionHash + """ and
        `index` = """ + index).list.head > 0
    }

  def initializeDB: Unit =
  {
    deleteIfExists(stats, movements, blockDB, addresses, richestAddresses, richestClosures, utxo)
    stats.ddl.create
    movements.ddl.create
    blockDB.ddl.create
    utxo.ddl.create
    addresses.ddl.create
    richestAddresses.ddl.create
    richestClosures.ddl.create
//    (Q.u + "alter table movements alter column address varbinary(401)").execute
//    (Q.u + "alter table addresses alter column hash varbinary(401)").execute
//    (Q.u + "alter table addresses alter column representant varbinary(401)").execute
//    (Q.u + "alter table movements alter column transaction_hash varbinary(32)").execute
//    (Q.u + "alter table movements alter column spent_in_transaction_hash varbinary(32)").execute
  }
}
