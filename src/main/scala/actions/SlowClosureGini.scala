package actions

import core._
import util._
import java.io._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 26.01.15.
 */
object SlowClosureGini
{
  transactionDBSession
  {
    val queried = addresses.filter(_.balance.isDefined).filter(_.balance =!= 0L).groupBy(_.representant).map{case (representant, group) => group.map(_.balance).sum}
   

    val n: Long = queried.length.run
    val balances = queried.run.toVector.map(_.get.toDouble).sorted
    val summe = balances.sum
    val mainSum = balances.zipWithIndex.map(p => p._1*(p._2+1.0)/n).sum
    val gini:Double = 2.0*mainSum/(summe) - (n+1.0)/n

    println(" TEST "+ gini)
  }
}
