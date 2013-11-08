package bddb

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.driver.MySQLDriver.simple._


object Outputs extends Table[(Int, Int, Int, Int)]("b_outputs") {

  def transaction_id = column[Int]("transaction_id")
  def address_id = column[Int]("address_id")
  def index = column[Int]("index")
  def value = column[Int]("value")
  def * = transaction_id ~ address_id ~ index ~ value
  //def idx1 = primaryKey("id", (transaction_id,index))
  //def idx2 = index("transaction_id", transaction_id)
  //def idx3 = index("address_id", address_id)
}

object Inputs extends Table[(Int, Int, Int)]("b_inputs") {

  def transaction_id = column[Int]("transaction_id")
  def address_id = column[Int]("address_id")
  def value = column[Int]("value")
  def * = transaction_id ~ address_id ~ value
  //def idx1 = index("transaction_id", transaction_id)
  //def idx2 = index("address_id", address_id)
}

object Transactions extends Table[(String, Int, Int)]("b_transactions") {

  def id= column[Int]("id",O.PrimaryKey)
  def block_id = column[Int]("block_id")
  def hash= column[String]("hash")
  def * = hash ~ id ~ block_id
  //def idx1 = index("hash", hash/*, unique = true*/)
  //def idx2 = index("block_id", block_id)
}
object Addresses extends Table[(String, Int)]("b_addresses") {

  def id= column[Int]("id",O.PrimaryKey)
  def hash= column[String]("hash")
  def * = hash ~ id
  //def idx = index("hash", hash, unique = true)
}
object Blocks extends Table[(String, Int)]("b_blocks") {

  def id= column[Int]("id",O.PrimaryKey)
  def hash= column[String]("hash")
  def * = hash ~ id
  //def idx = index("hash", hash, unique = true)

  def findByHash(hash: String)(implicit session: Session): Option[(String,Int)] = {
    val block = this.map { e => e }.where(u => u.hash === hash).take(1)
    block.firstOption
  }
}