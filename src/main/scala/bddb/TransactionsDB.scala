package bddb

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.driver.MySQLDriver.simple._


object TransactionsDB extends Table[(Int, Int, Int, String, String, String, String )]("transactions") {
  def block_nr = column[Int]("block_nr")
  def transaction_nr = column[Int]("transaction_nr")
  def movement_nr = column[Int]("movement_nr")
  def from = column[String]("from")
  def to = column[String]("to")
  def value = column[String]("value")
  def mode = column[String]("mode")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = block_nr ~ transaction_nr ~ movement_nr ~ from ~ to ~ value ~ mode
}
