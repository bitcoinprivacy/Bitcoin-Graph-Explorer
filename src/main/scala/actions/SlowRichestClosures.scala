package actions

import util._
import scala.slick.jdbc.{StaticQuery => Q}
//import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
  * Created by yzark on 22.12.14.
  */
object SlowRichestClosures {

  println("DEBUG: Calculating richest closure list...")                           
  val startTIme = System.currentTimeMillis   
   /**
    * Created by yzark on 15.12.14.
    */
  transactionDBSession {
    Q.updateNA( """
      insert
        into richest_closures
      select
        (select max(block_height) from blocks) as block_height,
        representant as address,
        sum(balance) as balance
      from
        addresses
      where
        balance > 0
      group by
        representant
      order by
        balance desc
      limit 1000
      ;""").execute
    Q.updateNA("create index if not exists richest2 on richest_closures(block_height);").execute

    println("DONE: Richest closure list calculated in " + (System.currentTimeMillis - startTIme)/1000 + "s")
  }
}
