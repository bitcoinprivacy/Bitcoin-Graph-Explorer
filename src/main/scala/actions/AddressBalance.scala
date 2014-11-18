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
  /*
  val numMovements = countInputs
  val amount: Int = 1000000
  for (begin <- 0 until numMovements by amount) {
    println("loading elements in " + begin + " - " + (amount + begin ))
    val x = System.currentTimeMillis
    process(begin, amount)
    println("loaded in " + (System.currentTimeMillis - x )*1000 + " s")
  }

  def process(begin: Int, amount: Int) = {
    transactionDBSession {
      val sumQuery = for {
        (address, c) <- movements.filterNot(q => q.spent_in_transaction_hash.isDefined).groupBy(_.address)
      } yield address -> c.map(_.value).sum

      for {(addressOption, balanceOption) <- sumQuery
           address <- addressOption}
        updateBalanceByAddress(address, balanceOption)
    }
  }

  def updateBalanceByAddress(address: Array[Byte], balance: Option[Long])() = {
    val q = for { l <- addresses if l.hash === address } yield l.balance
    if (q.length.run == 1)
      q.update(balance)
    else
      addresses += (address, address, balance)
  }    */
  println("DONE:Updating addresses ...")
  transactionDBSession {
    Q.updateNA("" +
      " UPDATE" +
      "  addresses " +
      "SET" +
      " balance = (select sum(value) from movements where spent_in_transaction_hash is null and address = addresses.hash group by address) " +
      ";").execute
    println("DONE:Addresses updated in %s s" format (System.currentTimeMillis - clock))
  }
}	