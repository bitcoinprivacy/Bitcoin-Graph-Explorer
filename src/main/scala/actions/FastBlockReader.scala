package actions

import java.io.File

import core._
import util._

import scala.collection.immutable

// for blocks db and longestChain
import com.google.bitcoin.core._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

trait FastBlockReader extends BlockReader
{
  // txhash -> ([address,...],[value,...]) (one entry per index)
  var outputMap: immutable.HashMap[Hash,immutable.HashMap[Int,(Hash,Double)]]  = immutable.HashMap()
  //  outpoint -> txhash
  var outOfOrderInputMap: immutable.HashMap[(Hash,Int),Hash]  = immutable.HashMap()
  var vectorMovements:
    Vector[(Option[Array[Byte]], Option[Array[Byte]], Option[Array[Byte]], Option[Int], Option[Double])] = Vector()
  var vectorBlocks:Vector[Array[Byte]]  = Vector()
  var totalOutIn: Int = 0

  def saveTransaction(trans: Transaction) =
  {
    val transactionHash = Hash(trans.getHash.getBytes)

    for (input <- inputsInTransaction(trans))
        includeInput(input,transactionHash)

    var index = 0
    var outputBuffer = immutable.HashMap[Int,(Hash,Double)]()

    for (output <- outputsInTransaction(trans))
    {
      val addressOption: Option[Array[Byte]] = getAddressFromOutput(output: TransactionOutput)
      val value = output.getValue.doubleValue

      if (outOfOrderInputMap.contains(transactionHash, index))
      {
        val inputTxHash = outOfOrderInputMap(transactionHash, index)
        insertInsertIntoList((inputTxHash.toSomeArray, transactionHash.toSomeArray, addressOption, Some(index), Some(value)))
        outOfOrderInputMap -= (transactionHash -> index)
      }
      else
      {
        val address = addressOption match
        {
          case Some(address) => Hash(address)
          case _ => Hash.zero(0)
        }

        outputBuffer += (index -> (address, value))
      }

      totalOutIn += 1
      index += 1
    }

    if (!outputBuffer.isEmpty)
      outputMap += (transactionHash -> outputBuffer)
  }

  def saveBlock(b: Hash) = {
    insertInsertIntoList(b.array.toArray)
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

    (Q.u + "create index if not exists address on movements (address)" + ";").execute
    (Q.u + "create unique index if not exists transaction_hash_i on movements (transaction_hash, `index`)" + ";").execute
    (Q.u + "create index if not exists spent_in_transaction_hash on movements (spent_in_transaction_hash)" + ";").execute
    System.out.println("Wir sind geil!")
  }

  def saveUnmatchedOutputs: Unit =
  {
    for ((transactionHash, indexMap) <- outputMap)
    {
      for ((index, (address, value)) <- indexMap)
        insertInsertIntoList (None, transactionHash.toSomeArray, address.toSomeArray, Some(index), Some(value))
      outputMap -= transactionHash
    }
  }

  def saveUnmatchedInputs: Unit =
  {
    for (((outpointTransactionHash, outpointIndex), transactionHash) <- outOfOrderInputMap)
      insertInsertIntoList (transactionHash.toSomeArray, outpointTransactionHash.toSomeArray, None, Some(outpointIndex), None)

  }

  def saveDataToDB: Unit =
  {
    blockDB.insertAll(vectorBlocks: _*)
    movements.insertAll(vectorMovements: _*)
    vectorMovements = Vector()
    vectorBlocks = Vector()
  }

  def insertInsertIntoList(s: Array[Byte]) =
  {
    if (vectorMovements.length + vectorBlocks.length >= populateTransactionSize)
      saveDataToDB
    vectorBlocks +:= s
  }

  def insertInsertIntoList(s: (Option[Array[Byte]], Option[Array[Byte]], Option[Array[Byte]], Option[Int], Option[Double])) =
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
      val outputTxMap = outputMap(outpointTransactionHash)
      if (outputTxMap.contains(outpointIndex))
      {
        insertInsertIntoList(
          (transactionHash.toSomeArray, outpointTransactionHash.toSomeArray, outputTxMap(outpointIndex)._1.toSomeArray, Some(outpointIndex), Some(outputTxMap(outpointIndex)._2)))
        val updatedTxMap = outputTxMap - outpointIndex

        if (updatedTxMap.isEmpty)
          outputMap -= outpointTransactionHash
        else
          outputMap += (outpointTransactionHash -> updatedTxMap)
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
