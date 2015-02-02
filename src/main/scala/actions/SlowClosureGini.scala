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
    
    implicit val GetByteArr = GetResult(r => r.nextBytes)
    val queried = Q.queryNA[(Array[Byte], Long)] ( """
      select representant, balance from addresses where balance is not null and balance > 80000000000
""")    
    println("vector done")
    val hashMap: HashMap[Hash, Double] = HashMap.empty
    var i = 0

    for (pair <- queried)
    {
      if (pair._1 != null)
      {
        val address = Hash(pair._1)
        val balance = pair._2.toDouble
        i+=1
    
        if (i==1000)
        {
          println("added 1000 elements to map");
          i=0
        }

        hashMap.update(address, balance + hashMap.getOrElse(address, 0.0))
      }
    }

    val arrayBalances = hashMap.values.toVector.sorted
    val n = hashMap.size
    println("calculating gini from " + n + " closures");
    val summe = arrayBalances.sum
    val mainSum = arrayBalances.zipWithIndex.map(p => p._1*(p._2+1.0)/n).sum
    val gini:Double = 2.0*mainSum/(summe) - (n+1.0)/n

    println("TEST: Total closures: " + n)
    println("TEST: Closure gini: "+ gini)
  }
}
