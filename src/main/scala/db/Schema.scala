package db

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.driver.PostgresDriver.simple._

class Blocks(tag:Tag) extends Table[(Array[Byte], Int, Int, Long, Long)](tag, "blocks") {
  def hash= column[Array[Byte]]("hash")
  def block_height = column[Int]("block_height")
  def txs = column[Int]("txs")
  def btcs = column[Long]("btcs")
  def tstamp = column[Long]("tstamp")
  def * =  (hash, block_height, txs, btcs, tstamp)

//  def idx1 = index("idx_1", (hash), unique = true)
//  def idx2 = index("idx_2", (block_height), unique = true)
}

class Stats(tag:Tag) extends Table[(Int, Long, Long, Long, Long, Long, Long, Long, Long, Double, Double, Long)](tag, "stats") {
  def block_height = column[Int]("block_height")
  def total_bitcoins_in_addresses = column[Long]("total_bitcoins_in_addresses")
  def total_transactions = column[Long]("total_transactions")
  def total_addresses = column[Long]("total_addresses")
  //def total_closured_addresses = column[Int]("total_closured_addresses")
  def total_closures = column[Long]("total_closures")
  def total_addresses_with_balance = column[Long]("total_addresses_with_balance")
  def total_closures_with_balance = column[Long]("total_closures_with_balance")
  def total_addresses_no_dust = column[Long]("total_addresses_no_dust")
  def total_closures_no_dust = column[Long]("total_closures_no_dust")
  def gini_closure = column[Double]("gini_closure")
  def gini_address = column[Double]("gini_address")
  def tstamp = column[Long]("tstamp")


  def * =  (block_height, total_bitcoins_in_addresses, total_transactions, total_addresses, total_closures, total_addresses_with_balance, total_closures_with_balance, total_addresses_no_dust, total_closures_no_dust, gini_closure, gini_address, tstamp)
}

class RichestAddresses(tag:Tag) extends Table[(Int, Array[Byte], Long)](tag, "richest_addresses") with HashField with BalanceField with BlockHeightField {
  def * =  (block_height, hash, balance)
  def idx1 = index("idxx1", (block_height), unique = false)
}

class RichestClosures(tag:Tag) extends Table[(Int, Array[Byte], Long)](tag, "richest_closures") with HashField with BalanceField with BlockHeightField {
    def * =  (block_height, hash, balance)
  def idx1 = index("idxx2", (block_height), unique = false)
}

class Addresses(tag:Tag) extends Table[(Array[Byte], Array[Byte])](tag, "addresses") {
  def hash= column[Array[Byte]]("hash")
  def representant = column[Array[Byte]]("representant")

  def * = (hash,representant)
}

trait BalanceField { this: Table[_] =>
  def balance = column[Long]("balance")
}

trait HashField  { this: Table[_] =>
  def hash = column[Array[Byte]]("hash")
}

trait BlockHeightField  { this: Table[_] =>
  def block_height = column[Int]("block_height")
}

trait AddressField  { this: Table[_] =>
  def address = column[Array[Byte]]("address", O.PrimaryKey)
}

class Balances(tag:Tag) extends Table[(Array[Byte], Long)](tag, "balances") with AddressField with BalanceField {
  def * = (address, balance)
}

class ClosureBalances(tag:Tag) extends Table[(Array[Byte], Long)](tag, "closure_balances") with AddressField with BalanceField {
  def * = (address, balance)
}


class Movements(tag:Tag) extends Table[(Array[Byte], Array[Byte], Array[Byte], Int, Long, Int, Int)](tag, "movements") {
  def transaction_hash = column[Array[Byte]]("transaction_hash")
  // Address can be a single byte 00 plus 20-byte address, or a 2-hex number plus several addresses
  def address = column[Array[Byte]]("address")
  def index = column[Int]("index")
  def value = column[Long]("value")
  def spent_in_transaction_hash = column[Array[Byte]]("spent_in_transaction_hash")
  def height_in = column[Int]("height_in")
  def height_out = column[Int]("height_out")

  //def idx1 = index("idx_1", (transaction_hash, index), unique = true)
  //def idx2 = index("idx_2", (address), unique = false)
  //def idx3 = index("idx_3", (spent_in_transaction_hash, address), unique = false)
  //def idx4 = index("idx_4", (block_height), unique = false)
  //def idx5 = index("idx_5", (transaction_hash, index), unique = false)
  //def idx6 = index("idx_6", (transaction_hash, index), unique = false)

  def * = (spent_in_transaction_hash,transaction_hash,address,index,value, height_in, height_out)
}

class UTXO(tag:Tag) extends Table[(Array[Byte], Array[Byte], Int, Long, Int)](tag, "utxo") {
  def transaction_hash = column[Array[Byte]]("transaction_hash")
  // Address can be a single byte 00 plus 20-byte address, or a 2-hex number plus several addresses
  // Non decoded addresses are represented as X'', since mysql cannot manage index over NULL values
  def address = column[Array[Byte]]("address")
  def index = column[Int]("index")
  def value = column[Long]("value")
  def block_height = column[Int]("block_height")

  def * = (transaction_hash,address,index,value, block_height)
}
