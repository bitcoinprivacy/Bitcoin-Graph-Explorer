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
    println("Drop and create balances"); 
    (Q.u + "drop table if exists balances;").execute
    (Q.u + "create table balances (balance double, address blob);").execute
    println("Inserting elements"); 
    (Q.u + "insert into balances SELECT sum(value) as balance, address as address FROM movements m  WHERE spent_in_transaction_hash IS NULL  GROUP BY address;").execute
    println("Creating indexes..."); 
    (Q.u + "create index address_balance on balances(address);").execute
    println("Drop and create closure_balances") 
    (Q.u + "drop table if exists closure_balance;").execute
    (Q.u + "create table closure_balance (members integer, representant blob, balance double);").execute
    println("Inserting elements")
    (Q.u + "insert into closure_balance select ifnull(count(distinct(a.hash)), 1) as members, hex(b.address) as address, sum(b.balance) total from balances b left outer join addresses a on a.hash = b.address group by a.representant;").execute
    (Q.u + "create index cb1 on closure_balance(members)").execute
    (Q.u + "create index cb2 on closure_balance(representant)").execute
    (Q.u + "create index cb3 on closure_balance(balance)").execute
  }
  println("     Balance table updated in %s ms" format (System.currentTimeMillis - clock))
}
