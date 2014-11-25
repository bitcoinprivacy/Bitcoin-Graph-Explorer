package actions

import util._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 24.11.14.
 */
class SlowAddressBalance(savedMovements: Vector[(Option[Array[Byte]], Option[Array[Byte]], Option[Array[Byte]], Option[Int], Option[Long], Option[Int])] = Vector())  {

  var clock = System.currentTimeMillis
  transactionDBSession {
    val byAddress = addresses.findBy(t => t.hash)
    for {q <- savedMovements}
      println(q)
    for {q <- savedMovements
      address <- q._3
      value <- q._5
    }{
      val updateQuery = for(p <- addresses if p.hash === address) yield p.balance
      val balance = updateQuery.firstOption

      balance match {
        case Some(Some(b)) => q._1 match {
          case None => updateQuery.update(Some(b+value))
          case Some(_) => updateQuery.update(Some(b-value))
        }
        case Some(None) => q._1 match {
          case None => updateQuery.update(Some(value))
          case Some(_) => updateQuery.update(Some(0))
        }
        case None => q._1 match {
          case None => addresses += (address, address, Some(value))
          case Some(_) => addresses += (address, address, Some(0))
        }
      }
    }
  }
  println("DONE: %s addresses updated in %s s, %s µs per address "
    format
      (savedMovements.length, (System.currentTimeMillis - clock)/1000, (System.currentTimeMillis - clock)*1000/(savedMovements.length+1)))
}
