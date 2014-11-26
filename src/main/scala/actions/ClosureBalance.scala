package actions

import util._
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

/**
 * Created by yzark on 10.11.14.
 */
object ClosureBalance {
  var clock = System.currentTimeMillis

  transactionDBSession {
    Q.updateNA("drop table if exists closures_temp").execute
    Q.updateNA("create table closures_temp (representant blob, members integer, balance integer)").execute
    Q.updateNA("insert into closures_temp SELECT a.representant as representant, count(*) as members, sum(ifnull(b.balance,0)) as balance FROM addresses a left join balances b on a.hash = b.address group by a.representant;").execute
    Q.updateNA("drop table if exists closures").execute
    Q.updateNA("alter table closures_temp rename to closures").execute
  }

  println("DONE: Closures table updated in %s ms" format (System.currentTimeMillis - clock))
}
