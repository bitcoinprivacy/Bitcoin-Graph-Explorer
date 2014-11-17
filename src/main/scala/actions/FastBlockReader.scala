package actions

import java.io.File

import core._
import util._

import scala.collection.immutable

// for blocks db and longestChain
import org.bitcoinj.core._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

trait FastBlockReader extends BlockReader
{
  // txhash -> ([address,...],[value,...]) (one entry per index)
  var outputMap: immutable.HashMap[Hash,(immutable.HashMap[Int,(Hash,Long)], Int)]  = immutable.HashMap()
  //  outpoint -> txhash
  var outOfOrderInputMap: immutable.HashMap[(Hash,Int),Hash]  = immutable.HashMap()
  var vectorMovements:
    Vector[(Option[Array[Byte]], Option[Array[Byte]], Option[Array[Byte]], Option[Int], Option[Long], Option[Int])] = Vector()
  var vectorBlocks:Vector[(Array[Byte], Int)]  = Vector()
  var totalOutIn: Int = 0

  def useDatabase: Boolean = true

  def saveTransaction(trans: Transaction, blockHeight: Int) =
  {
    val transactionHash = Hash(trans.getHash.getBytes)

    for (input <- inputsInTransaction(trans))
        includeInput(input,transactionHash)

    var index = 0
    var outputBuffer = immutable.HashMap[Int,(Hash,Long)]()

    for (output <- outputsInTransaction(trans))
    {
      val addressOption: Option[Array[Byte]] = getAddressFromOutput(output: TransactionOutput)
      val value = output.getValue.value

      if (outOfOrderInputMap.contains(transactionHash, index))
      {
        val inputTxHash = outOfOrderInputMap(transactionHash, index)

        insertInsertIntoList(inputTxHash.toSomeArray, transactionHash.toSomeArray, addressOption, Some(index), Some(value), Some(blockHeight))
        insertInsertIntoList(inputTxHash.toSomeArray, transactionHash.toSomeArray, addressOption, Some(index), Some(value), Some(blockHeight))
        outOfOrderInputMap -= (transactionHash -> index)
      }
      else
      {
        val address = addressOption match
        {
          case Some(address) => Hash(address)
          case _ =>             Hash.zero(0)
        }

        outputBuffer += (index -> (address, value))
      }

      totalOutIn += 1
      index += 1
    }

    if (!outputBuffer.isEmpty)
      outputMap += (transactionHash -> ((outputBuffer, blockHeight)))
  }

  def saveBlock(b: Hash) = {
    insertInsertIntoList(b.array.toArray, longestChain.getOrElse(b,0))
  }

  def pre  = {
    outputMap = immutable.HashMap()
    outOfOrderInputMap = immutable.HashMap()
    vectorMovements = Vector()
    vectorBlocks = Vector()
    totalOutIn = 0
    System.out.println("Sind wir geil?")
    initializeDB
}

  def post = {
    saveUnmatchedOutputs
    saveUnmatchedInputs
    saveDataToDB

    Q.updateNA("create index if not exists address on movements (address)" + ";").execute
    Q.updateNA("create unique index if not exists transaction_hash_i on movements (transaction_hash, `index`)" + ";").execute
    Q.updateNA("create index if not exists spent_in_transaction_hash on movements (spent_in_transaction_hash)" + ";").execute
    Q.updateNA("create index if not exists block_hash on blocks(hash)").execute
    System.out.println("Wir sind geil!")
  }

  def saveUnmatchedOutputs: Unit =
  {
    for ((transactionHash, (indexMap, blockHeight)) <- outputMap)
    {
      for ((index, (address, value)) <- indexMap)
        insertInsertIntoList (None, transactionHash.toSomeArray, address.toSomeArray, Some(index), Some(value),Some(blockHeight))
      outputMap -= transactionHash
    }
  }

  def saveUnmatchedInputs: Unit =
  {
    for (((outpointTransactionHash, outpointIndex), transactionHash) <- outOfOrderInputMap)
      insertInsertIntoList (transactionHash.toSomeArray, outpointTransactionHash.toSomeArray, None, Some(outpointIndex), None, None)

  }

  def saveDataToDB: Unit =
  {
    blockDB.insertAll(vectorBlocks: _*)
    movements.insertAll(vectorMovements: _*)
    vectorMovements = Vector()
    vectorBlocks = Vector()
  }

  def insertInsertIntoList(s: (Array[Byte], Int)) =
  {
    if (vectorMovements.length + vectorBlocks.length >= populateTransactionSize)
      saveDataToDB
    vectorBlocks +:= s
  }

  def insertInsertIntoList(s: (Option[Array[Byte]], Option[Array[Byte]], Option[Array[Byte]], Option[Int], Option[Long], Option[Int])) =
  {
    if (vectorMovements.length + vectorBlocks.length >= populateTransactionSize)
      saveDataToDB
    vectorMovements +:= s
  }

  def includeInput(input: TransactionInput, transactionHash: Hash) =
  {
    val outpointTransactionHash = Hash(input.getOutpoint.getHash.getBytes)
    val outpointIndex = input.getOutpoint.getIndex.toInt

    if (outputMap.contains(outpointTransactionHash))
    {
      val (outputTxMap,blockHeight) = outputMap(outpointTransactionHash)

      if (outputTxMap.contains(outpointIndex))
      {
        insertInsertIntoList(
          (transactionHash.toSomeArray, outpointTransactionHash.toSomeArray, outputTxMap(outpointIndex)._1.toSomeArray, Some(outpointIndex), Some(outputTxMap(outpointIndex)._2), Some(blockHeight)))
        val updatedTxMap = outputTxMap - outpointIndex

        if (updatedTxMap.isEmpty)
          outputMap -= outpointTransactionHash
        else
          outputMap += (outpointTransactionHash -> (updatedTxMap,blockHeight))
      }
    }
    else
    {
      outOfOrderInputMap += ((outpointTransactionHash, outpointIndex) -> transactionHash)
    }

    totalOutIn += 1
  }

  def initializeDB: Unit =
  {
    movements.ddl.create
    blockDB.ddl.create
    addresses.ddl.create
  }
} 
