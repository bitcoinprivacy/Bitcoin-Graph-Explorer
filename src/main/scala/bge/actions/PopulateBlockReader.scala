package actions

import slick.driver.PostgresDriver.simple._
import core.FastBlockReader
import util._
import Hash._

object PopulateBlockReader extends FastBlockReader with core.BitcoinDRawFileBlockSource {
  // txhash -> ((index -> (address,value)),blockIn)
  // need to override this here to get the specialized type
  override lazy val table: LmdbMap = LmdbMap.create("utxos")

  override def post = {
    saveUnmatchedOutputs
    super.post
    table.close
  }

  def saveUnmatchedOutputs: Unit =
    for (group <- outputMap.view.grouped(populateTransactionSize))
      DB withSession { implicit session =>
        utxo.insertAll((for (((transactionHash,index), (address, value, blockHeight)) <- group.toSeq)
                        yield (hashToArray(transactionHash),hashToArray(address), index, value,blockHeight)):_*)
      }
  
  // Here's a puzzle:
  // the following code was here before. It is ugly, but it used to work. Then it didn't. There is a nullpointer exception in the inner insertAll line.
  // Only in the inner one. No exception when the outer one is run. We cannot figure out why.
  //
  // def saveUnmatchedOutputs: Unit =
  // {
  //   var vectorUTXO:  Vector[(Array[Byte], Array[Byte], Int, Long, Int)] = Vector()
    
  //   for (((transactionHash,index), (address, value, blockHeight)) <- outputMap.view){
      
  //     vectorUTXO +:= (transactionHash.array.toArray, address.array.toArray, index, value,blockHeight)
  //     if (vectorUTXO.size == populateTransactionSize){
  //       utxo.insertAll(vectorUTXO:_*) // Nullpointer exception here
  //       vectorUTXO = Vector()
  //     }
  //   }

  //   utxo.insertAll(vectorUTXO:_*) // but not here, in the case where the if-condition is never true

  // }

}
