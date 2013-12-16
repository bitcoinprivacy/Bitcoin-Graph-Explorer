package libs

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.driver.MySQLDriver.simple._

object RawBlocks extends Table[(String)]("blocks") {

  def hash= column[String]("hash")
  def * = hash
  //def idx = index("hash", hash, unique = true)

  //def findByHash(hash: String)(implicit session: Session): Option[(String,Int)] = {
  //  val block = this.map { e => e }.where(u => u.hash === hash).take(1)
  //  block.firstOption
  //}
}

object RawOutputs extends Table[(String, String, Int, Double)]("outputs") {

  def transaction_hash = column[String]("transaction_hash")
  def address = column[String]("address")
  def index = column[Int]("index")
  def value = column[Double]("value")
  def * = transaction_hash ~ address ~ index ~ value
  //def inpoint = index("inpoint", (transaction_hash, index), unique = true)
  //def idx2 = index("transaction_hash", transaction_hash)
  //def idx3 = index("address", address)
}

object RawInputs extends Table[(String, Int, String)]("inputs") {

  def output_transaction_hash = column[String]("output_transaction_hash")
  def output_index = column[Int]("output_index")
  def transaction_hash = column[String]("transaction_hash")
  //def idx2 = index("transaction_hash", transaction_hash)
  //def outpoint = index("outpoint", (output_transaction_hash, output_index), unique = true)
  def * = output_transaction_hash ~ output_index ~ transaction_hash

}

object GroupedAddresses extends Table[(String, String, Double)]("grouped_addresses") {

  def hash= column[String]("hash")
  def representant = column[String]("representant")
  def balance= column[Double]("balance")
  def * = hash ~ representant ~ balance
  //def idx = index("hash", hash, unique = true)
  //def idx2 = index("representant", representant)
}

