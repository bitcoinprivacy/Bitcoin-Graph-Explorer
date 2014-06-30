/**
 * Created by yzark on 12/16/13.
 */

import com.typesafe.config.ConfigFactory
import scala.slick.driver.SQLiteDriver.simple._
import Database.threadLocalSession
import scala.slick.jdbc.{StaticQuery => Q}


package object libs
{  
  val conf = ConfigFactory.load()
  var transactionsDatabaseFile = conf.getString("transactionsDatabaseFile") //"blockchain/bitcoin.db"
  var addressesDatabaseFile = conf.getString("addressesDatabaseFile") //"blockchain/bitcoin.db"
  var closureTransactionSize = conf.getInt("closureTransactionSize")
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

  def getCurrentLongestBlockChainHashes(): Set[Hash] =
  {
    Set(
      Hash("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"),
      Hash("00000000839a8e6886ab5951d76f411475428afc90947ee320161bbf18eb6048")
    )
  }

  def countInputs: Int =
  {
    Q.queryNA[Int]("""select count(*) from movements""").list.head
  }

}
