package actions

import core._
import util._
import java.io._
import scala.collection.mutable
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 26.01.15.
 */
object SlowClosureGini
{
  def apply = {
    transactionDBSession
    {
      println("Getting gini of closures")
    
      //    val queried = Q.queryNA[Long] ("select sum(balance) as balance from addresses where balance > 546 group by representant order by balance")
      // val queried = addresses.filter(_.balance.isDefined).filter(_.balance > dustLimit).groupBy(_.representant).map{
      //   case (name,c) => c.map(_.balance).sum }
    
      // val n: Long = queried.length.run
      // val balances = queried.run.toVector.map(_.get.toDouble).sorted
      // val summe = balances.sum
      // val mainSum = balances.zipWithIndex.map(p => p._1*(p._2+1.0)/n).sum
      // val gini:Double = 2.0*mainSum/(summe) - (n+1.0)/n

      // println("TEST: Total closures: " + n)
      // println("TEST: Closure gini: "+ gini)
      //Q.updateNA(" update stats set gini_closure = " + gini + " order by block_height desc limit 1").execute
    }
  }
}
