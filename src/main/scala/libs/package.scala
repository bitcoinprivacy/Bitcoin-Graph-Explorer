/**
 * Created by yzark on 12/16/13.
 */

import com.typesafe.config.ConfigFactory
import scala.slick.driver.SQLiteDriver.simple._
import Database.threadLocalSession
import scala.slick.jdbc.{StaticQuery => Q}
import java.io._

package object libs
{  
  val conf = ConfigFactory.load()
  var transactionsDatabaseFile = conf.getString("transactionsDatabaseFile") //"blockchain/bitcoin.db"
  var addressesDatabaseFile = conf.getString("addressesDatabaseFile") //"blockchain/bitcoin.db"
  var closureTransactionSize = conf.getInt("closureTransactionSize")
  var closureReadSize = conf.getInt("closureReadSize")
  var populateTransactionSize = conf.getInt("populateTransactionSize")
  var balanceTransactionSize = conf.getInt("balanceTransactionSize")

  def transactionsDBSession(f: => Unit): Unit =
  {
    Database.forURL(
      url = "jdbc:sqlite:"+transactionsDatabaseFile,
      driver = "org.sqlite.JDBC"
    ) withSession
    {
      (Q.u + "PRAGMA main.page_size = 4962;"    ).execute
      (Q.u + "PRAGMA main.cache_size=10000;"    ).execute
      (Q.u + "PRAGMA main.locking_mode=NORMAL;" ).execute
      (Q.u + "PRAGMA main.synchronous=OFF;"     ).execute
      (Q.u + "PRAGMA main.journal_mode=OFF;"    ).execute
      f
    }    
  }

  def addressesDBSession(f: => Unit): Unit =
  {
    Database.forURL(
      url = "jdbc:sqlite:"+addressesDatabaseFile,
      driver = "org.sqlite.JDBC"
    ) withSession
      {
        (Q.u + "PRAGMA main.page_size = 4962;"    ).execute
      (Q.u + "PRAGMA main.cache_size=10000;"    ).execute
      (Q.u + "PRAGMA main.locking_mode=NORMAL;" ).execute
      (Q.u + "PRAGMA main.synchronous=OFF;"     ).execute
      (Q.u + "PRAGMA main.journal_mode=OFF;"    ).execute
        f
      }
  }

  val arrayNull = Hash.zero(1).array.toArray

  def countInputs: Int =
  {
    Q.queryNA[Int]("""select count(*) from movements""").list.head
  }

  def getLongestBlockChainHashSet: Set[Hash] =
  {
    val lines = scala.io.Source.fromFile("blockchain/blocklist.txt").getLines
    val hashes = for (line <- lines) yield Hash(line)

    hashes.toSet
  }

  private var filename: String = null
  private var logfile: File = null
  private var writer: FileWriter = null
  def println(s: String) = if (filename != null) writeLogger(s) else System.out.print(s)
  def startLogger(s: String) =
  {
    filename = s
    logfile = new File("logs/"+filename+".log")
    writer = new FileWriter(logfile)
  }
  private def writeLogger(s: String): Unit =
  {
    writer = new FileWriter(logfile, true)
    writer.write(s+"\n")
    writer.close()
  }
  def stopLogger() = {filename = null}
}