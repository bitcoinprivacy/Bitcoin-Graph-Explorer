package libs

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.driver.SQLiteDriver.simple._

class Blocks(tag:Tag) extends Table[(Array[Byte])](tag, "blocks") {

  def hash= column[Array[Byte]]("hash")
  def * =  hash
}

class Addresses(tag:Tag) extends Table[(Array[Byte], Array[Byte], Option[Double])](tag, "addresses") {

  def hash= column[Array[Byte]]("hash")
  def representant = column[Array[Byte]]("representant")
  def balance= column[Option[Double]]("balance", O.Nullable)

  def * = (hash,representant,balance)
}

class Outputs(tag:Tag) extends Table[(Option[Array[Byte]], Option[Array[Byte]], Option[Array[Byte]], Option[Int], Option[Double])](tag, "movements") {

  def transaction_hash = column[Option[Array[Byte]]]("transaction_hash", O.Nullable)
  def address = column[Option[Array[Byte]]]("address", O.Nullable)
  def index = column[Option[Int]]("index", O.Nullable)
  def value = column[Option[Double]]("value", O.Nullable)
  def spent_in_transaction_hash = column[Option[Array[Byte]]]("spent_in_transaction_hash", O.Nullable)

  def * = (spent_in_transaction_hash,transaction_hash,address,index,value)
}