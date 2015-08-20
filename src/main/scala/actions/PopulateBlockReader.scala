package actions

import scala.collection.JavaConversions._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._
import util.Hash._


object PopulateBlockReader extends FastBlockReader
{
  // txhash -> ((index -> (address,value)),blockIn)
  override lazy val table: LmdbMap = LmdbMap.create("utxos")

  override def post = {
    saveUnmatchedOutputs
    super.post
    table.commit
  }

  def saveUnmatchedOutputs: Unit =
  {
    for (i <- 0  until outputMap.size - populateTransactionSize by populateTransactionSize)
    {
      var vectorUTXO:  Vector[(Array[Byte], Array[Byte], Int, Long, Int)] = Vector()
      for (((transactionHash,index), (address, value, blockHeight)) <- outputMap.slice(i, i+populateTransactionSize))
      {
          vectorUTXO +:= (transactionHash.array.toArray, address.array.toArray, index, value,blockHeight)
      }

      utxo.insertAll(vectorUTXO:_*)
    }
  }

}
