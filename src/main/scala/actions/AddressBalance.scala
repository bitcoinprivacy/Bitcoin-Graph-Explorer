package actions

import util._
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 10.09.14.
 */
object AddressBalance {
  var clock = System.currentTimeMillis

  transactionDBSession {
    Q.updateNA("BEGIN TRANSACTION")
    println("Drop and create balances");
    Q.updateNA("DROP TABLE IF EXISTS balances;").execute
    Q.updateNA("CREATE TABLE balances (address blob, balance double, representant blob);").execute
    println("Inserting elements");
    Q.updateNA(
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
      GROUP BY address;
    """).
      execute
    println("Inserting elements");
    // queries having "having" do not terminate propertly with slick!
    Q.updateNA(
      """
      INSERT INTO balances SELECT
        m.address as address,
        0 as balance,
        m.address as representant
      FROM
        movements m left join
        (SELECT count(*) as count, address from movements where spent_in_transaction_hash is NULL) t on t.address = m.address
      WHERE
        t.count IS NULL
      GROUP BY m.address
      ;
    """).
      execute
    println("Inserting elements");
    Q.updateNA(
      """
      INSERT INTO balances SELECT
        m.address as address,
        sum(m.value) as balance,
        m.address as representant
      FROM
        movements m left outer join addresses a on m.address = a.hash
      WHERE
        spent_in_transaction_hash IS NULL and a.representant is null
      GROUP BY address;
    """).execute;
    println(
      "Creating indexes...");
    Q.updateNA(
      "CREATE INDEX b1 ON balances(address);").execute
    Q.updateNA(
      "CREATE INDEX b2 ON balances(representant);").execute
    Q.updateNA("COMMIT TRANSACTION")
  }

  println("     Balance table updated in %s ms" format (System.currentTimeMillis - clock))
}
