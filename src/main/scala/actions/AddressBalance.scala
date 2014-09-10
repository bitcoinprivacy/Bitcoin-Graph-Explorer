package actions

import util._
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 10.09.14.
 */
object AddressBalance
{
  // TODO run these queries as one to avoid data requests during the balance refresh operation
  var clock = System.currentTimeMillis
  transactionDBSession
  {
    (Q.u + "drop table if exists balances;").execute
    (Q.u + "create table balances (balance double, address blob);").execute
    (Q.u + "insert into balances SELECT sum(value) as balance, address as address FROM movements m  WHERE spent_in_transaction_hash IS NULL  GROUP BY address;").execute
    (Q.u + "create index address_balance on balances(address);").execute
  }
  println("     Balance table updated in %s ms" format (System.currentTimeMillis - clock))
}
