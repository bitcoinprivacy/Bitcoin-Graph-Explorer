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
        (select max(block_height) from blocks) as block_height,
        sum(balance)/100000000 as total_bitcoins_in_addresses,
        (select count(1) from addresses) as total_addresses,
        (select count(distinct(representant)) from addresses)  as total_closures,
        count(1) as total_addresses_with_balance,
        count(distinct(representant)) as total_closures_with_balance,
        (select count(1) from addresses where balance > 546) as total_addresses_no_dust,
        (select count(distinct(representant)) from addresses where balance > 546) as total_closures_no_dust
      From
        addresses
      where
        balance > 0
    ;""").execute
   
    Q.updateNA("create index if not exists stats1 on stats(block_height);")
    println("DONE: Stats calculated in " + (System.currentTimeMillis - startTIme)/1000 + "s");
  }
}



