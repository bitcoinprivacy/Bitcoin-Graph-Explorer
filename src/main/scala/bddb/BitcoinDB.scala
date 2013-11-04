package bddb

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.driver.MySQLDriver.simple._


object Outputs extends Table[(String, Long, Int, String )]("outputs") {

  def transaction_hash = column[String]("transaction_hash")
  def index = column[Long]("index")
  def value = column[Int]("value")
  def address = column[String]("address")

  // Every table needs a * projection with the same type as the table's type parameter
  def * = transaction_hash ~ index ~ value ~ address
}

object Inputs extends Table[(String, Long, Int, String, String, Long)]("inputs") {

  def transaction_hash = column[String]("transaction_hash")
  def index = column[Long]("index")
  def value = column[Int]("value")
  def address = column[String]("address")
  def outpoint_transaction_hash = column[String]("outpoint_transaction_hash")
  def outpoint_index = column[Long]("outpoint_index")

  // Every table needs a * projection with the same type as the table's type parameter
  def * = transaction_hash ~ index ~ value ~ address ~ outpoint_transaction_hash ~ outpoint_index
}
