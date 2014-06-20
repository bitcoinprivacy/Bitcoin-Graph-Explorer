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
  var databaseFile = conf.getString("databaseFile") //"blockchain/bitcoin.db"
  var closureTransactionSize = conf.getInt("closureTransactionSize")
  var populateTransactionSize = conf.getInt("populateTransactionSize")
  var balanceTransactionSize = conf.getInt("balanceTransactionSize")

  def databaseSession(f: => Unit): Unit =
  {
    Database.forURL(
      url = "jdbc:sqlite:"+databaseFile,
      driver = "org.sqlite.JDBC"
    ) withSession
    {
      (Q.u + "PRAGMA main.page_size = 4096;"    ).execute
      (Q.u + "PRAGMA main.cache_size=10000;"    ).execute
      (Q.u + "PRAGMA main.locking_mode=NORMAL;" ).execute
      (Q.u + "PRAGMA main.synchronous=OFF;"     ).execute
      (Q.u + "PRAGMA main.journal_mode=OFF;"    ).execute
      f
    }    
  }
  
  def countInputs: Int =
  {
    Q.queryNA[Int]("""select count(*) from movements""").list.head
  }

}
