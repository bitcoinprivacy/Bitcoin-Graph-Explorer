package actions

import util._
import core._
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession


/**
 * Created by yzark on 10.09.14.
 */
object FastAddressBalance extends BitcoinDB {
  var clock = System.currentTimeMillis
  transactionDBSession {
    println("DEBUG: Updating addresses ...")
    Q.updateNA("" +
      " UPDATE IGNORE" +
      "  addresses " +
      "SET" +
      " balance = (select sum(value) from movements where spent_in_transaction_hash is null and address = addresses.hash group by address) " +
      ";").execute

    Q.updateNA("INSERT IGNORE " +
      " INTO addresses select " +
      "   address, address, sum(value) " +
      " FROM " +
      "   movements " +
      " WHERE " +
      " spent_in_transaction_hash is null group by address" +
      ";").execute;

    (Q.u + "create index addresses_balance on addresses(balance)").execute

    println("DONE: Addresses updated in %s s" format (System.currentTimeMillis - clock)/1000)
  }
}
