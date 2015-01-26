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
object SlowAddressGini
{
  transactionDBSession
  {
    val queried = addresses.filter(_.balance.isDefined).filter(_.balance =!= 0L).sortBy(_.balance.desc).map(_.balance)

    val summe: Long = queried.sum.asInstanceOf[Long]
    val n: Long = queried.length.asInstanceOf[Long]
    val balances = queried.asInstanceOf[Vector[Long]]
    val mainSum = balances.zipWithIndex.map(p => p._1*p._2).sum
    val gini:Double = 2.0*mainSum/(n*summe) - (n+1.0)/n

    println(" TEST "+ gini)
  }
}
