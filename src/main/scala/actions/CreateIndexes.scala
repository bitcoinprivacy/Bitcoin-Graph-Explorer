package actions

import util._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{StaticQuery => Q}

object CreateIndexes {

  println("DEBUG: Creating indexes ...")
  val time = System.currentTimeMillis

  transactionDBSession {

    // MOVEMENTS

    // get outputs from address
    Q.updateNA("create index address on movements (address);").execute
    // used to query the movements in a transaction
    Q.updateNA("create unique index tx_idx  on movements (transaction_hash, `index`);").execute
    // get input from an output
    Q.updateNA("create index  spent_in_transaction_hash2 on movements (spent_in_transaction_hash, address);").execute
    // to get transactions from a block
    Q.updateNA("create index height_in on movements (height_in);").execute
    // to get the balance of an address at a certain height
    Q.updateNA("create index height_out_in on movements (height_out, height_in);").execute

    // BLOCKS

    // just for querying block info
    Q.updateNA("create index  block_hash on blocks(hash);").execute
    Q.updateNA("create index  block_height on blocks(block_height);").execute

    // ADDRESSES

    // to query the addresses in a wallet
    Q.updateNA("create index representant on addresses (representant)").execute
    // to query the representant of an address
    Q.updateNA("create unique index hash on addresses (hash)").execute
  }

  println("DONE: Indexes created in %s s" format (System.currentTimeMillis - time)/1000)

}
