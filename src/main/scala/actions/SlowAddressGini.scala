package actions

import core._
import util._
import java.io._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 26.01.15.
 */
object SlowAddressGini
{
  def apply = {
    transactionDBSession
    {
      println("Getting address gini")
      val queried = addresses.filter(_.balance.isDefined).filter(_.balance > dustLimit).sortBy(_.balance.asc).map(_.balance)

      val n: Long = queried.length.run
      val balances = queried.run.toVector.map(_.get.toDouble)
      val summe = balances.sum
      val mainSum = balances.zipWithIndex.map(p => p._1*(p._2+1.0)/n).sum
      val gini:Double = 2.0*mainSum/(summe) - (n+1.0)/n

      println("TEST: Total addresses: " + n)
      println("TEST: Address gini: "+ gini)
      Q.updateNA(" update stats set gini_address = " + gini + " order by block_height desc limit 1").execute
    }
  }
}
