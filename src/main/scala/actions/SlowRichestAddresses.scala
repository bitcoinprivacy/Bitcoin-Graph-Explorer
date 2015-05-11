package actions

import util._
import scala.slick.jdbc.{StaticQuery => Q}
//import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 22.12.14.
 */
object SlowRichestAddresses {

  println("DEBUG: Calculating richest address list...")
  val startTIme = System.currentTimeMillis
  /**
   * Created by yzark on 15.12.14.
   */
  transactionDBSession {
    Q.updateNA( """
       insert
        into richest_addresses
       select
        (select max(block_height) from blocks) as block_height,
        hash,
        balance
      from
        addresses
      order by
        balance desc
      limit 1000
    ;""").execute

    println("DONE: Richest address list calculated in " + (System.currentTimeMillis - startTIme)/1000 + "s")
  }
}
