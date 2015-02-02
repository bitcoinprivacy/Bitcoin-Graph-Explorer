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
    case class Pairr(representant:Array[Byte], balance: Long)
    implicit val getUserResult = GetResult(r => Pairr(r.nextBytes, r.nextLong))
    val queried = Q.queryNA[Pairr] ("select representant, balance from addresses")    
    val hashMap: HashMap[Hash, Double] = HashMap.empty

    for (pair <- queried)
    {
      val balance = pair.balance.toDouble
      
      if (pair.representant != null && balance > 546 )
      {
        val address = Hash(pair.representant)
        hashMap.update(address, balance + hashMap.getOrElse(address, 0.0))
      }
    }

    val arrayBalances = hashMap.values.toVector.sorted
    val n = hashMap.size
    val summe = arrayBalances.sum
    val mainSum = arrayBalances.zipWithIndex.map(p => p._1*(p._2+1.0)/n).sum
    val gini:Double = 2.0*mainSum/(summe) - (n+1.0)/n

    println("TEST: Total closures: " + n)
    println("TEST: Closure gini: "+ gini)
    Q.updateNA(" update stats set gini_closure = " + gini + " where block_height = (select max(block_height) from stats)").execute
  }
}
