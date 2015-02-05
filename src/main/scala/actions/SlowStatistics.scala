package actions

import util._
import scala.slick.jdbc.{StaticQuery => Q}
//import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 15.12.14.
 */
object SlowStatistics {
  // TODO: write output
  println("DEBUG: Calculating stats...")
  val startTIme = System.currentTimeMillis
  transactionDBSession {
    Q.updateNA( """
       insert
        into stats
       select
        (select max(block_height) from blocks),
        sum(balance)/100000000,
        0,
        (select count(1) from addresses),
        (select count(distinct(representant)) from addresses),
        count(1),
        count(distinct(representant)),
        (select count(1) from addresses where balance > 546),
        (select count(distinct(representant)) from addresses where balance > 546),
        0, 
        0 
      from
        addresses
      where
        balance > 0
    ;""").execute
   
    Q.updateNA("create index if not exists stats1 on stats(block_height);").execute
    println("DONE: Stats calculated in " + (System.currentTimeMillis - startTIme)/1000 + "s");
  }
}



