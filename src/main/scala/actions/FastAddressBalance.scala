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

    Q.updateNA("insert into balances select address, sum(value) as balance from utxo group by address;").execute

    (Q.u + "create index addresses_balance on balances(address)").execute
    (Q.u + "create index balance on balances(balance)").execute

    Q.updateNA("insert into closure_balances select a.representant, sum(b.balance) as balance from balances b, addresses a where b.address = a.hash group by a.representant;").execute

    (Q.u + "create index addresses_balance_2 on closure_balances(representant)").execute
    (Q.u + "create index balance_2 on closure_balances(balance)").execute

    println("DONE: Balances updated in %s s" format (System.currentTimeMillis - clock)/1000)
  }
}
