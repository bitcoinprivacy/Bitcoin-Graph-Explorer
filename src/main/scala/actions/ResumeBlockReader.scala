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
  override lazy val table: LmdbMap = LmdbMap.open("utxos")

  var deleteQuery = utxo.filter(_.transaction_hash === hashToArray(Hash.zero(0))) //should be empty
  var deleteCounter = 0

  override def removeUTXO(outpointTransactionHash: util.Hash, outpointIndex: Int): UTXOs = {
    val a:  Array[Byte] = outpointTransactionHash
    deleteQuery ++ utxo.filter(p => (p.transaction_hash === a && p.index === outpointIndex))
    deleteCounter += 1
    if (deleteCounter >= populateTransactionSize)
    {
      println ("deleting")
      deleteQuery.delete
      deleteQuery = utxo.filter(_.transaction_hash === hashToArray(Hash.zero(0))) //should be empty
      deleteCounter = 0
      println ("done deleting")
    }
    outputMap -= (outpointTransactionHash -> outpointIndex)

  }

  override def addUTXO(blockHeight: Int, transactionHash: util.Hash, index: Int, value: Long, address: util.Hash): UTXOs = {
    insertUTXO((transactionHash,address,index,value,blockHeight))
    outputMap += ((transactionHash,index) -> (address, value, blockHeight))
  }

  override def pre = {
    super.pre
    vectorUTXOs = Vector()
    deleteQuery = utxo.filter(_.transaction_hash === hashToArray(Hash.zero(0))) //should be empty
  }

  override def post = {
    println("finishing ...")
    stop
    saveUTXOs
    deleteQuery.delete
    super.post
    table.close
  }

  var vectorUTXOs: Vector[(Hash, Hash, Int, Long, Int)] = Vector()

  def insertUTXO(s: (Hash, Hash, Int, Long, Int)) =
  {
    vectorUTXOs +:= s

    if (vectorUTXOs.length >= populateTransactionSize)
      saveUTXOs
  }

  def saveUTXOs = {
    println("DEBUG: Inserting UTXOs into SQL database ...")

    if (vectorUTXOs.length > 0)
    {
      def vectorUTXOConverter[A,B,C](v:Vector[(Hash,Hash,A,B,C)]) = v map {
        case (a,b,c,d,e) => (hashToArray(a),hashToArray(b),c,d,e) }

      val convertedVectorUTXOs = vectorUTXOConverter(vectorUTXOs)

      utxo.insertAll(convertedVectorUTXOs:_*)
    }

    vectorUTXOs = Vector()

    println("DEBUG: Data inserted")

  }
}
