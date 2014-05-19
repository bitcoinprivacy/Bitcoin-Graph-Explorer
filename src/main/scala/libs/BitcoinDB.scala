package libs

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.driver.SQLiteDriver.simple._

object RawBlocks extends Table[(String)]("blocks") {

  def hash= column[String]("hash")
  def * =  hash
}

object RawOutputs extends Table[(String, String, Int, Double)]("outputs") {

  def transaction_hash = column[String]("transaction_hash")
  def address = column[String]("address")
  def index = column[Int]("index")
  def value = column[Double]("value")
  def * = transaction_hash ~ address ~ index ~ value
  def xa = transaction_hash ~ index
//  def pk = primaryKey("pk_myTable2", (transaction_hash, index) )
}

object RawInputs extends Table[(String, Int, String, Int)]("inputs") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def output_transaction_hash = column[String]("output_transaction_hash")
  def output_index = column[Int]("output_index")
  def transaction_hash = column[String]("transaction_hash")
  //def fkMyTable1 = foreignKey("myTable1_fk", output_transaction_hash ~ output_index, RawOutputs)( _.xa )
  def * = output_transaction_hash ~ output_index ~ transaction_hash ~ id
}

object Addresses extends Table[(String, String, Double)]("addresses") {

  def hash= column[String]("hash")
  def representant = column[String]("representant")
  def balance= column[Double]("balance")
  def * = hash ~ representant ~ balance
}

