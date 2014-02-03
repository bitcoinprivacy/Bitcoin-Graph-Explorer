/**
 * Created by yzark on 12/16/13.
 */
import scala.slick.driver.SQLiteDriver.simple._
import Database.threadLocalSession
import scala.collection.mutable.HashMap
import scala.slick.jdbc.meta.MTable
import scala.collection.JavaConversions._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

import java.lang.instrument._

package object libs {
  val db_file = "/home/yzark/Repositories/Bitcoin-Graph-Explorer/blockchain/bitcoin.db"

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

  var globalInstr:Instrumentation= null;
  def premain(args:String, inst: Instrumentation) {
    globalInstr = inst
  }
}
