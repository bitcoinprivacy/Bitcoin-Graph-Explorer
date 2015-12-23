package actions

import scala.collection.JavaConversions._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import core.FastBlockReader
import util._
import util.Hash._


object PopulateBlockReader extends FastBlockReader with core.BitcoinDRawFileBlockSource
{
  // txhash -> ((index -> (address,value)),blockIn)
  // need to override this here to get the specialized type
  override lazy val table: LmdbMap = LmdbMap.create("utxos")

  override def post = {
    saveUnmatchedOutputs
    super.post
    table.close
  }

  def saveUnmatchedOutputs: Unit =
  {
    var vectorUTXO:  Vector[(Array[Byte], Array[Byte], Int, Long, Int)] = Vector()
    for (((transactionHash,index), (address, value, blockHeight)) <- outputMap.iterator){
      vectorUTXO +:= (transactionHash.array.toArray, address.array.toArray, index, value,blockHeight)
      if (vectorUTXO.size == populateTransactionSize){
        utxo.insertAll(vectorUTXO:_*)
        vectorUTXO = Vector()
      }
    }
    utxo.insertAll(vectorUTXO:_*)

  }

}
