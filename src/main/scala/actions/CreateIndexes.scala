package actions

import util._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{StaticQuery => Q}

object CreateIndexes {

  println("DEBUG: Creating indexes ...")
  val time = System.currentTimeMillis

  transactionDBSession {
    Q.updateNA("create index  address on movements (address (20));").execute
    Q.updateNA("create unique index  transaction_hash_i on movements (transaction_hash (20), `index`);").execute
    Q.updateNA("create index  spent_in_transaction_hash2 on movements (spent_in_transaction_hash (20), address(20));").execute
    Q.updateNA("create index  block_height on movements (block_height);").execute
    Q.updateNA("create index  block_hash on blocks(hash (20));").execute
    Q.updateNA("create index  block_height2 on blocks(block_height);").execute
  }

  println("DONE: Indexes created in %s s" format (System.currentTimeMillis - time)/1000)

}
