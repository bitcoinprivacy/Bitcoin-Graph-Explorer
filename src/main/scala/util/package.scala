/**
 * Created by yzark on 12/16/13.
 */

import java.io._

import com.typesafe.config.ConfigFactory
import core._

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{StaticQuery => Q}

package object util
{  
  val conf = ConfigFactory.load()

  var transactionsDatabaseFile = conf.getString("transactionsDatabaseFile") //"blockchain/bitcoin.db"
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

  def transactionDBSession[X](f: => X): X =
  {
    Database.forURL(
      url = "jdbc:sqlite:"+transactionsDatabaseFile,
      driver = "org.sqlite.JDBC"
    ) withDynSession
    {
      (Q.u + "PRAGMA main.page_size = 4962;"    ).execute
      (Q.u + "PRAGMA main.cache_size=1000000;"    ).execute
      (Q.u + "PRAGMA main.locking_mode=NORMAL;" ).execute
      (Q.u + "PRAGMA main.synchronous=NORMAL;"     ).execute
      (Q.u + "PRAGMA main.journal_mode=WAL;"    ).execute
      f
    }    
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
