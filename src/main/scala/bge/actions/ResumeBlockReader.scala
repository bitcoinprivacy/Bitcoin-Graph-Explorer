package actions

import core._
import scala.collection.JavaConversions._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._
import util.Hash._
import collection.mutable.{HashMap, Map} 

class ResumeBlockReader extends FastBlockReader with PeerSource
{
  // txhash -> ((index -> (address,value)),blockIn)
  override lazy val table: LmdbMap = LmdbMap.open("utxos")
  lazy val changedAddresses: HashMap[Hash, Long] = HashMap() // todo: change this into an option which is none if the change would be too big
  var deletedUTXOs: Vector[(Array[Byte], Int)] = Vector()

  override def removeUTXO(outpointTransactionHash: util.Hash, outpointIndex: Int): UTXOs = {
    val a:  Array[Byte] = outpointTransactionHash
    deletedUTXOs +:= (outpointTransactionHash.array.toArray, outpointIndex)
    //utxo.filter(p => (p.transaction_hash === a && p.index === outpointIndex)).delete

    if (deletedUTXOs.length >= 100)
      deleteUTXOs

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

  def deleteUTXOs = {
    for ( (e,i) <- deletedUTXOs.headOption) {
      utxo.filter( p => deletedUTXOs.drop(1).foldLeft(p.transaction_hash === e && p.index === i){
                    case (found,(tx,id)) => found || (p.transaction_hash === tx && p.index === id)
                  }).delete
    }
    deletedUTXOs = Vector()
  }

  override def pre = {
    super.pre
    deletedUTXOs = Vector()
  }

  override def post = {
    println("finishing ...")
    deleteUTXOs
    saveUTXOs
    super.post
    table.close
  }

  lazy val newUtxos = Map[(Hash,Int),(Hash,Long,Int)]()

  def insertUTXO(s: (Hash, Hash, Int, Long, Int)) =
  {
    newUtxos += (s._1,s._3) -> (s._2,s._4,s._5)

    if (newUtxos.size >= populateTransactionSize)
      saveUTXOs
  }

  def saveUTXOs = {
    println("DEBUG: Inserting UTXOs into SQL database ...")

    def vectorUTXOConverter[A,B,C](v:Map[(Hash,A),(Hash,B,C)]) = v map {
      case ((a,b),(c,d,e)) => (hashToArray(a),hashToArray(c),b,d,e) }

    val convertedVectorUTXOs = vectorUTXOConverter(newUtxos).toSeq

    utxo.insertAll(convertedVectorUTXOs:_*)

    newUtxos.clear

    println("DEBUG: Data inserted")

  }
}
