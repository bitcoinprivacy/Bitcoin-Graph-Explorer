package actions

import util._
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.driver.SQLiteDriver.simple._

/**
 * Created by yzark on 10.09.14.
 */
object AddressBalance {
  var clock = System.currentTimeMillis

  // the table balances contains a list of address with balance > 0
  // if an address is not here, then he has 0 btc.
  transactionDBSession {
    val numMovements = countInputs
    val amount: Int = 1000000
    for (begin <- 0 until numMovements by amount) {
      println("loading elements in " + begin + " - " + (amount + begin ))
      val x = System.currentTimeMillis
      process(begin, amount)
      println("loaded in " + (System.currentTimeMillis - x )*1000 + " s")
    }
  }

  def process(begin: Int, amount: Int) = {
    val sumQuery = for {
      (address, c) <- movements.filterNot(q => q.spent_in_transaction_hash.isDefined).groupBy(_.address).drop(begin).take(amount)
    } yield address -> c.map(_.value).sum

    for {(addressOption, balanceOption) <- sumQuery
         address <- addressOption}
      updateBalanceByAddress(address,balanceOption)
  }

  def updateBalanceByAddress(address: Array[Byte], balance: Option[Long])() = {
    val q = for { l <- addresses if l.hash === address } yield l.balance
    if (q.length.run == 1)
      q.update(balance)
    else
      addresses += (address, address, balance)
  }



  println("     Balance table updated in %s ms" format (System.currentTimeMillis - clock))
}	