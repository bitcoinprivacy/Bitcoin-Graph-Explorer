package actions

import core._
import scala.collection.JavaConversions._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._
import util.Hash._
import collection.mutable.Map

class ResumeBlockReader extends FastBlockReader with PeerSource
{
  // txhash -> ((index -> (address,value)),blockIn)
  override lazy val table: LmdbMap = LmdbMap.open("utxos")
  lazy val changedAddresses: Map[Hash, Long] = Map()
 // var deleteQuery = utxo.filter(_.transaction_hash === hashToArray(Hash.zero(0))) //should be empty
 // var deleteCounter = 0

  override def removeUTXO(outpointTransactionHash: util.Hash, outpointIndex: Int): UTXOs = {
    val a:  Array[Byte] = outpointTransactionHash
    //deleteQuery = deleteQuery ++
    utxo.filter(p => (p.transaction_hash === a && p.index === outpointIndex)).delete
    //deleteCounter += 1
    // if (deleteCounter >= populateTransactionSize)
    // {
    //   println ("deleting")
    //   deleteQuery.delete
    //   deleteQuery = utxo.filter(_.transaction_hash === hashToArray(Hash.zero(0))) //should be empty
    //   deleteCounter = 0
    //   println ("done deleting")
    // }

    val (address, value, _) = outputMap((outpointTransactionHash, outpointIndex))
    val newValue = changedAddresses.getOrElse(address, 0L)-value
    changedAddresses += (address -> newValue)
    newUtxos -= outpointTransactionHash -> outpointIndex
    outputMap -= outpointTransactionHash -> outpointIndex
  }

  override def addUTXO(blockHeight: Int, transactionHash: util.Hash, index: Int, value: Long, address: util.Hash): UTXOs = {
    val newValue = changedAddresses.getOrElse(address, 0L)+value
    changedAddresses += (address -> newValue)
    // copy to database.
    insertUTXO((transactionHash,address,index,value,blockHeight))
    outputMap += ((transactionHash,index) -> (address, value, blockHeight))
  }

  override def pre = {
    super.pre
    
  //  deleteQuery = utxo.filter(_.transaction_hash === hashToArray(Hash.zero(0))) //should be empty
  }

  override def post = {
    println("finishing ...")
    stop
    saveUTXOs
  //  deleteQuery.delete
    super.post
    table.close
  }

  lazy val newUtxos:Map[(Hash,Int),(Hash,Long,Int)]  = new UTXOs(Map[Hash,Hash]())

  def insertUTXO(s: (Hash, Hash, Int, Long, Int)) =
  {
    newUtxos += (s._1,s._3) -> (s._2,s._4,s._5)

    if (newUtxos.size >= populateTransactionSize)
      saveUTXOs
  }

  def saveUTXOs = {
    println("DEBUG: Inserting UTXOs into SQL database ...")

    if (newUtxos.size > 0)
    {
      def vectorUTXOConverter[A,B,C](v:Map[(Hash,A),(Hash,B,C)]) = v map {
        case ((a,b),(c,d,e)) => (hashToArray(a),hashToArray(c),b,d,e) }

      val convertedVectorUTXOs = vectorUTXOConverter(newUtxos).toSeq

      utxo.insertAll(convertedVectorUTXOs:_*)
    }

    newUtxos.clear

    println("DEBUG: Data inserted")

  }
}
