package actions

import util._
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import core.BitcoinDB
/**
 * Created by yzark on 15.12.14.
 */
class SlowStatistics(addressGini: Double, closureGini: Double) extends BitcoinDB {
  // TODO: write output

    println("DEBUG: Calculating stats...")

    val startTime = System.currentTimeMillis
    transactionDBSession {
      val query =   """
       insert
        into stats select
        (select max(block_height) from blocks),
        (select sum(balance)/100000000 from balances),
        (select sum(txs) from blocks),
        (select count(1) from addresses),
        (select count(distinct(representant)) from addresses),
        (select count(1) from balances),
        (select count(1) from closure_balances),
        (select count(1) from balances where balance > 546),
        (select count(1) from closure_balances where balance > 546),
        """+closureGini.toString+""",
        """+addressGini.toString+""",
        """+ (System.currentTimeMillis/1000).toString +""";"""
      println(query)

  (Q.u + query).execute
      println("DONE: Stats calculated in " + (System.currentTimeMillis - startTime)/1000 + "s");

  }
}
