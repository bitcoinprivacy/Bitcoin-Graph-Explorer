package actions

import util._
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 10.09.14.
 */
object AddressBalance {
  var clock = System.currentTimeMillis

  // the table balances contains a list of address with balance > 0
  // if an address is not here, then he has 0 btc.
  transactionDBSession {
    println("Drop and create balances");
    Q.updateNA("drop table if exists balances_temp;").execute
    Q.updateNA("create table balances_temp (balance double, address blob);").execute
    println("Inserting elements");
    Q.updateNA("insert into balances_temp SELECT sum(value) as balance, address as address FROM movements m  WHERE spent_in_transaction_hash IS NULL  GROUP BY address;").execute
    println("Creating indexes...");
    Q.updateNA("drop table if exists balances;").execute
    Q.updateNA("ALTER TABLE balances_temp RENAME TO balances;").execute
    Q.updateNA("create unique index address_balance_ on balances(address, balance);").execute
    //Q.updateNA("drop table if exists closures")
    //Q.updateNA("create table closures (representant blob, hash block, balance integer)").execute
    //Q.updateNA("insert into closures SELECT a.representant as representant, count(*) as members, sum(ifnull(b.balance,0)) as balance FROM addresses a left join balances b on a.hash = b.address group by a.representant;").execute
  }

  println("     Balance table updated in %s ms" format (System.currentTimeMillis - clock))
}