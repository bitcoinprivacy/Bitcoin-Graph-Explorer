package core

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.driver.SQLiteDriver.simple._

class Blocks(tag:Tag) extends Table[(Array[Byte], Int)](tag, "blocks") {
  def hash= column[Array[Byte]]("hash")
  def block_height = column[Int]("block_height")
  def * =  (hash, block_height)
}

class Stats(tag:Tag) extends Table[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Double, Double)](tag, "stats") {
  def block_height = column[Int]("block_height")
  def total_bitcoins_in_addresses = column[Int]("total_bitcoins_in_addresses")
  def total_transactions = column[Int]("total_transactions")
  def total_addresses = column[Int]("total_addresses")
  def total_closures = column[Int]("total_closures")
  def total_addresses_with_balance = column[Int]("total_addresses_with_balance")
  def total_closures_with_balance = column[Int]("total_closures_with_balance") 
  def total_addresses_no_dust = column[Int]("total_addresses_no_dust")
  def total_closures_no_dust = column[Int]("total_closures_no_dust")
  def gini_closure = column[Double]("gini_closure")
  def gini_address = column[Double]("gini_address")
  def * =  (block_height, total_bitcoins_in_addresses, total_transactions, total_addresses, total_closures, total_addresses_with_balance, total_closures_with_balance, total_addresses_no_dust, total_closures_no_dust, gini_closure, gini_address)
}

class RichestAddresses(tag:Tag) extends Table[(Int, Array[Byte], Int)](tag, "richest_addresses") {
  def block_height = column[Int]("block_height")
  def hash= column[Array[Byte]]("hash")
  def balance= column[Int]("balance", O.Nullable)
  def * =  (block_height, hash, balance)
}

class RichestClosures(tag:Tag) extends Table[(Int, Array[Byte], Int)](tag, "richest_closures") {
  def block_height = column[Int]("block_height")
  def hash= column[Array[Byte]]("hash")
  def balance= column[Int]("balance", O.Nullable)
  def * =  (block_height, hash, balance)
}

class Addresses(tag:Tag) extends Table[(Array[Byte], Array[Byte], Option[Long])](tag, "addresses") {
  def hash= column[Array[Byte]]("hash")
  def representant = column[Array[Byte]]("representant")
  def balance= column[Option[Long]]("balance", O.Nullable)

  def * = (hash,representant,balance)
}

class Movements(tag:Tag) extends Table[(Option[Array[Byte]], Option[Array[Byte]], Option[Array[Byte]], Option[Int], Option[Long], Option[Int])](tag, "movements") {
  def transaction_hash = column[Option[Array[Byte]]]("transaction_hash", O.Nullable)
  // Address can be a single byte 00 plus 20-byte address, or a 2-hex number plus several addresses
  def address = column[Option[Array[Byte]]]("address", O.Nullable)
  def index = column[Option[Int]]("index", O.Nullable)
  def value = column[Option[Long]]("value", O.Nullable)
  def spent_in_transaction_hash = column[Option[Array[Byte]]]("spent_in_transaction_hash", O.Nullable)
  def block_height = column[Option[Int]]("block_height", O.Nullable)

  def * = (spent_in_transaction_hash,transaction_hash,address,index,value, block_height)
}
