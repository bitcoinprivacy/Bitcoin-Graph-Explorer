package actions

import util._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{StaticQuery => Q}

object CreateIndexes {

  println("DEBUG: Creating indexes ...")
  val time = System.currentTimeMillis

  transactionDBSession {

    // MOVEMENTS

    // get outputs from address
    for (query <- List(
      "create index address on movements (address);",
      """create unique index tx_idx  on movements (transaction_hash, "index");""",
      "create index  spent_in_transaction_hash2 on movements (spent_in_transaction_hash, address);",
      "create index height_in on movements (height_in);",
      "create index height_out_in on movements (height_out, height_in);",
      "create index address_utxo on utxo (address)",
      "create index height_utxo on utxo (block_height)",
      "create index  block_height on blocks(block_height);"))

    {
      Q.updateNA(query).execute
      println("DEBUG: Finished"+ query)
    }

  }

  println("DONE: Indexes created in %s s" format (System.currentTimeMillis - time)/1000)

}
