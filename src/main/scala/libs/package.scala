/**
 * Created by yzark on 12/16/13.
 */
import scala.slick.driver.SQLiteDriver.simple._
import Database.threadLocalSession
import scala.slick.jdbc.{StaticQuery => Q}

package object libs {
  val db_file = "/home/yzark/Repositories/Bitcoin-Graph-Explorer/blockchain/bitcoin.db"
  val stepClosure = 50000
  val stepPopulate = 50000

  def databaseSession(f: => Unit): Unit = {
    Database.forURL(
      url = "jdbc:sqlite:"+db_file,
      driver = "org.sqlite.JDBC"
     // user = "root",
      //password = "12345"
    ) withSession
    {
      (Q.u + "PRAGMA temp_store=OFF").execute
      (Q.u + "PRAGMA page_size = 400000;").execute
      (Q.u + "PRAGMA journal_mode=off;").execute
      (Q.u + "PRAGMA synchronous=0;").execute
      (Q.u + "PRAGMA cache_size=400000;").execute
      f
    }

  }


}
