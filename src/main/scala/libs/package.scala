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
  var stepClosure = conf.getInt("closureStep")
  var stepPopulate = conf.getInt("populateStep")
  var stepBalance = conf.getInt("balanceStep")

  def databaseSession(f: => Unit): Unit =
  {
    Database.forURL(
      url = "jdbc:sqlite:"+databaseFile,
      driver = "org.sqlite.JDBC"
    ) withSession
    {
      /*
      Test if exclusive lock and journal WAL helps.
      Someone in stackoverflow told he works with up to 120GB database with these config
      (Q.u + "PRAGMA main.page_size = 4096;"    ).execute
      (Q.u + "PRAGMA main.cache_size=10000;"    ).execute
      (Q.u + "PRAGMA main.locking_mode=EXCLUSIVE;"    ).execute
      (Q.u + "PRAGMA main.synchronous=NORMAL;"    ).execute
      (Q.u + "PRAGMA main.journal_mode=WAL;"    ).execute
      (Q.u + "PRAGMA main.cache_size=5000;"    ).execute*/
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
