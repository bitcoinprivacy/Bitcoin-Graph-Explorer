package actions

import util._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 24.11.14.
 */
class SlowAddressBalance(savedMovements: Map[(Array[Byte],Int),(Option[Array[Byte]],Option[Array[Byte]],Option[Long],Option[Int])])  {

  var clock = System.currentTimeMillis
  transactionDBSession {
    val byAddress = addresses.findBy(t => t.hash)
    for {(k,q) <- savedMovements
      address <- q._2
      value <- q._3
    }{
      val updateQuery = for(p <- addresses if p.hash === address) yield p.balance
      val balance = updateQuery.firstOption

      balance match { 
        case Some(Some(b)) => q._1 match { // there is address and balance
          case None => updateQuery.update(Some(b+value))
          case Some(_) => updateQuery.update(Some(b-value))
        }
        case Some(None) => q._1 match {    // there is address but no balance
          case None => updateQuery.update(Some(value))
          case Some(_) => updateQuery.update(Some(0))
        }
        case None => q._1 match {          // there is no address in DB
          case None => addresses += (address, address, Some(value)) // new unspent output
          case Some(_) => addresses += (address, address, Some(0)); println("ERROR: spent output, but not in closure?")  
        }
      }
    }
    
    println("DONE: %s addresses updated in %s s, %s µs per address "
    format
      (savedMovements.size, (System.currentTimeMillis - clock)/1000, (System.currentTimeMillis - clock)*1000/(savedMovements.size+1)))
  }
}
