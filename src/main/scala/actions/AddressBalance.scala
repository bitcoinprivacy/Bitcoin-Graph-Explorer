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
    (Q.u + "DROP TABLE IF EXISTS balances;").execute
    (Q.u + "CREATE TABLE balances (address blob, balance double, representant blob);").execute
    println("Inserting elements"); 
    (Q.u +
      """
        INSERT INTO balances
        SELECT
        	m.address as address,
        	sum(m.value) as balance,
        	a.representant as representant
        FROM
        	movements m left outer join addresses a on m.address = a.hash
        WHERE
        	spent_in_transaction_hash IS NULL and a.representant is not null
        GROUP BY address
        UNION
        SELECT
        	m.address as address,
        	sum(m.value) as balance,
        	m.address as representant
        FROM
        	movements m left outer join addresses a on m.address = a.hash
        WHERE
        	spent_in_transaction_hash IS NULL and a.representant is null
        GROUP BY address
        UNION
        SELECT
        	m.address as address,
        	0 as balance,
        	m.address as representant
        FROM
        	movements m
        GROUP BY address
        HAVING
        	(SELECT count(*) from movements where spent_in_transaction_hash is NULL AND address = m.address) = 0
        ;
      """).execute
    println("Creating indexes...");
    (Q.u + "CREATE INDEX b1 ON balances(address);").execute
    (Q.u + "CREATE INDEX b2 ON balances(representant);").execute
  }
  println("     Balance table updated in %s ms" format (System.currentTimeMillis - clock))
}
