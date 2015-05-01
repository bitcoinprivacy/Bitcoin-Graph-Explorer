/**
 * Created by yzark on 12/16/13.
 */

import java.io._

import com.typesafe.config.ConfigFactory
import core._

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.meta.MTable

package object util
{  
  val conf = ConfigFactory.load()

  var closureTransactionSize = conf.getInt("closureTransactionSize")
  var closureReadSize = conf.getInt("closureReadSize")
  var populateTransactionSize = conf.getInt("populateTransactionSize")
  var balanceTransactionSize = conf.getInt("balanceTransactionSize")
  var blockHashListFile= conf.getString("blockHashListFile")
  var dustLimit = conf.getLong("dustLimit")

  val blockDB = TableQuery[Blocks]
  val addresses = TableQuery[Addresses]
  val movements = TableQuery[Movements]
  val richestAddresses = TableQuery[RichestAddresses]
  val richestClosures = TableQuery[RichestClosures]
  val stats = TableQuery[Stats]

  val USERNAME="root"
  val PASSWORD=sys.env("MYSQL_ENV_MYSQL_ROOT_PASSWORD")
  val DBNAME=conf.getString("databaseName")
  val URL="jdbc:mysql://"+sys.env("MYSQL_PORT_3306_TCP_ADDR")+":"+sys.env("MYSQL_PORT_3306_TCP_PORT")+"/"+DBNAME+"?useServerPrepStmts=false&rewriteBatchedStatements=true&maxWait=-1"
//  val URL="jdbc:mysql://localhost:3306/movements"
  val DRIVER="com.mysql.jdbc.Driver"
 
  def deleteIfNotExists(tables: TableQuery[_ <: Table[_]]*)(implicit session: Session) {
  tables foreach {table => if(!MTable.getTables(table.baseTableRow.tableName).list.isEmpty) table.ddl.drop}
  }

  def transactionDBSession[X](f: => X): X =
  {
    Database.forURL(URL, user=USERNAME,password=PASSWORD,driver = DRIVER) withDynSession { f }
  }

  val arrayNull = Hash.zero(1).array.toArray

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

  def getLongestBlockChainHashSet: Map[Hash,Int] =
  {
    val lines = scala.io.Source.fromFile(blockHashListFile).getLines
    val hashes = for (line <- lines) yield Hash(line)
    hashes.zipWithIndex.toMap
  }
}
