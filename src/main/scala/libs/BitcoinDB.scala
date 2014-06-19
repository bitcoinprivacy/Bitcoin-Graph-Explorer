package libs

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.driver.SQLiteDriver.simple._

object Blocks extends Table[(String)]("blocks") {

  def hash= column[String]("hash")
  def * =  hash
}

object Addresses extends Table[(Array[Byte], Array[Byte], Double)]("addresses") {

  def hash= column[Array[Byte]]("hash")
  def representant = column[Array[Byte]]("representant")
  def balance= column[Double]("balance")

  def * = hash ~ representant ~ balance
}

object Outputs extends Table[(Array[Byte], Array[Byte], Array[Byte], Int, Double)]("movements") {

  def transaction_hash = column[Array[Byte]]("transaction_hash", O.Nullable)
  def address = column[Array[Byte]]("address", O.Nullable)
  def index = column[Int]("index", O.Nullable)
  def value = column[Double]("value", O.Nullable)
  def spent_in_transaction_hash = column[Array[Byte]]("spent_in_transaction_hash", O.Nullable)

  def * = spent_in_transaction_hash ~ transaction_hash ~ address ~ index ~ value
}