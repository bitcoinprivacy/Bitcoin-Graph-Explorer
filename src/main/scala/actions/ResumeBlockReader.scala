package actions

import core._
import scala.collection.JavaConversions._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._
import util.Hash._

class ResumeBlockReader extends FastBlockReader with PeerSource
{
  // txhash -> ((index -> (address,value)),blockIn)
  override val table: LmdbMap = LmdbMap.open("utxos")

  override def removeUTXO(outpointTransactionHash: util.Hash, outpointIndex: Int): UTXOs = {
    val a:  Array[Byte] = outpointTransactionHash.array.toArray
    val query = for (u <-  utxo.filter(p => (p.transaction_hash === a && p.index === outpointIndex))) yield(u)
    query.delete

    outputMap -= (outpointTransactionHash -> outpointIndex)

  }

 override def addUTXO(blockHeight: Int, transactionHash: util.Hash, index: Int, value: Long, address: util.Hash): UTXOs = {
    utxo.insert((transactionHash.array.toArray,address.array.toArray,index,value,blockHeight))
    outputMap += ((transactionHash,index) -> (address, value, blockHeight))
  }

  override def post = {
    super.post
    table.commit
  }

}
