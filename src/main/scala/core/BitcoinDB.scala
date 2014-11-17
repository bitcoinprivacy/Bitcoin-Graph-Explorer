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