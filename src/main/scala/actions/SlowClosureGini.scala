package actions

import core._
import util._
import java.io._
import scala.collection.mutable
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
    val queried = addresses.filter(_.balance.isDefined).filter(_.balance > 546).sortBy(_.balance.asc).map(x => (x.representant,x.balance))
   
    val hashMap: HashMap[Hash, Long] = HashMap.empty
    for (pair <- queried)
    {
      val x: Array[Byte] = pair._1
      val y: Long = pair._2.getOrElse(0L)
      hashMap.put(Hash(x), y + hashMap.getOrElse(Hash(x), 0L))
    }

    val n = hashMap.size
    val summe = hashMap.values.sum
    val mainSum = hashMap.values.zipWithIndex.map(p => p._1*(p._2+1.0)/n).sum
    val gini:Double = 2.0*mainSum/(summe) - (n+1.0)/n

    println("TEST: Total closures: " + n)
    println("TEST: Closure gini: "+ gini)
  }
}
