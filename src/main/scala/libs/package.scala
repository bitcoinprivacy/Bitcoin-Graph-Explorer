/**
 * Created by yzark on 12/16/13.
 */
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import scala.collection.mutable.HashMap
import scala.slick.jdbc.meta.MTable
import scala.collection.JavaConversions._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

package object libs {
  def databaseSession(f: => Unit): Unit = {
    Database.forURL(
      url = "jdbc:sqlite:/home/yzark/Repositories/Bitcoin-Graph-Explorer/blockchain/bitcoin.db",
      driver = "org.sqlite.JDBC"
     // user = "root",
      //password = "12345"
    ) withSession {
        (Q.u + "PRAGMA page_size = 409600;").execute
        (Q.u + "PRAGMA journal_mode=off;").execute
        (Q.u + "PRAGMA synchronous=0;").execute
        (Q.u + "PRAGMA cache_size=5000000;").execute
        (Q.u + "BEGIN TRANSACTION;").execute
        f
        (Q.u + "COMMIT TRANSACTION;").execute
    }

  }
}
