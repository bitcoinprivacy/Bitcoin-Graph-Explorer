import scala.slick.driver.SQLiteDriver.simple._

trait SQLSchemata {

object Blocks extends Table[(Array[Byte])]("blocks") {

  def hash= column[Array[Byte]]("hash")
  def * =  hash
}

object Addresses extends Table[(Array[Byte], Array[Byte], Double)]("addresses") {

  def address = column[Array[Byte]]("address")
  def representant = column[Array[Byte]]("representant")
  def balance = column[Double]("balance")

  def * = address ~ representant ~ balance
}

object Outputs extends Table[(Array[Byte], Array[Byte], Array[Byte], Int, Double)]("txos") {

  def tx_hash = column[Array[Byte]]("tx_hash", O.Nullable)
  def address = column[Array[Byte]]("address", O.Nullable)
  def index = column[Int]("index", O.Nullable)
  def value = column[Double]("value", O.Nullable)
  def spent_in_tx = column[Array[Byte]]("spent_in_tx", O.Nullable)

  def * = spent_in_tx ~ tx_hash ~ address ~ index ~ value
}
}