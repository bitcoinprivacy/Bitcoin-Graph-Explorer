package actions

import core._
import util._
import scala.slick.driver.PostgresDriver.simple._
import java.io._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import java.util.Calendar

/**
 * Created by yzark on 26.01.15.
 */
object SlowAddressGini extends BitcoinDB {
  def execute: (Double, Double) = {
    transactionDBSession(
    {

/*
      println("Getting address gini")
      val table = LmdbMap.create("balances")
      val balanceMap = new BalanceMap(table)

      val emptyArray = Hash.zero(0).array.toArray

      val queried = for {
        (address, rest) <- utxo.filter(_.address =!= emptyArray) groupBy (_.address)
      } yield address -> rest.map(_.value).sum

      for {
        (address, balanceOption) <- queried
        balance <- balanceOption
      }
        {
        val addressHash = Hash(address)

        balanceMap += (addressHash -> balance)

      }


      table.commit
 */
/*      println("Getting closure gini")
      val table2 = LmdbMap.create("closure_balances")
      val closureBalanceMap = new BalanceMap(table2)
      // select b.representant representant, sum(a.value) balance from utxo a join addresses b on a.address = b.address group by representant
      val queried2 = for {
        a <- addresses
        u <- utxo

        if (a.hash === u.address)

      } yield{
        (a.representant, u.value)
      }

      val queried3 = for {
        (representant, rest) <- queried2.groupBy (_._1)
      } yield representant -> rest.map(_._2).sum


      for {
        (representant, valueOpt) <- queried3
        value <- valueOpt
      }
      {
        closureBalanceMap += (Hash(representant) -> value)
      }


        //groupBy (_.representant)
      //yield rest.map(p => utxo.filter(_.address === p.hash).map(_.value).sum)


      println("saved" + Calendar.getInstance().getTime())
      //maxQuery.list
 */
      println("queried " + Calendar.getInstance().getTime())
     val vectorBalances = for (bal <- balances)
      yield bal.balance

      val (n, gini) = getGini(vectorBalances.run.toVector)
/*   val (m, closureGini) = getGini(closureBalanceMap)

      println("TEST: Total addresses: " + n)
      println("TEST: Address gini: "+ gini)

      val queried2 = for {
        a <- addresses
        b <- balances

        if (a.hash === b.address)

      } yield{
        (a.representant, b.balance)
      }

      val queried3 = for {
        (representant, rest) <- queried2.groupBy (_._1)
      } yield rest.map(_._2).sum*/

      val vectorClosureBalances = for (el <- closureBalances)
      yield el.balance


      val (m,closureGini) = getGini(vectorClosureBalances.run.toVector)

      println("TEST: Total closures: " + m)
      println("TEST: Closure gini: "+ closureGini)

      //Q.updateNA(" update stats set gini_address = " + gini + " order by block_height desc limit 1").execute

      (gini, closureGini)
    })


  }

  def getGini(balanceMap: Vector[Long]): (Long, Double) = {//val balances = queried.toVector.map(_.getOrElse(0L).toDouble).filter(_ > dustLimit).sorted
    val balances = balanceMap.filter(_ > dustLimit).map(_.toDouble).sorted
    //val queried = addresses.filter(_.balance.isDefined).filter(_.balance > dustLimit).sortBy(_.balance.asc).map(_.balance)
    println("vectored " + Calendar.getInstance().getTime())
    val n: Long = balances.length
    // val balances = queried.run.toVector.map(_.get.toDouble)
    val summe = balances.sum
    val mainSum = balances.zipWithIndex.map(p => p._1*(p._2+1.0)/n).sum
    val gini:Double = 2.0*mainSum/(summe) - (n+1.0)/n
    (n, gini)
  }
}
