package actions

import java.io.File

import core._
import util._
import util.Hash._

import scala.collection._
import scalax.file.{Path,FileSystem}

import scala.slick.jdbc.meta._
import org.bitcoinj.core._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

import java.nio.ByteBuffer

import collection.JavaConversions._


trait FastBlockReader extends BlockReader
{
  // txhash -> ((index -> (address,value)),blockIn)
  var outputMap: immutable.Map[(Hash,Int),(Hash,Long,Int)] = new UTXOs (immutable.HashMap[Hash,Hash]())


  //DBMaker.heapDB.transactionDisable.asyncWriteFlushDelay(100).make.getHashMap[(Hash,Int),(Hash,Long,Int)]("utxo")

  //  outpoint -> (txhash,block_out)
  var outOfOrderInputMap: immutable.HashMap[(Hash,Int),(Hash,Int)]  = immutable.HashMap()
  var vectorMovements: Vector[(Hash, Hash, Option[Hash], Int, Long, Int, Int)] = Vector()
  var vectorBlocks: Vector[(Hash, Int, Int, Long, Long)]  = Vector()

  var totalOutIn: Int = 0

  def useDatabase: Boolean = true

  def saveTransaction(trans: Transaction, blockHeight: Int) =
  {
    val transactionHash = Hash(trans.getHash.getBytes)

    val addresses =
      for {input <- inputsInTransaction(trans)
        address <- includeInput(input,transactionHash, blockHeight)
      }
      yield address



    var index = 0

    for (output <- outputsInTransaction(trans))
    {
      val addressOption: Option[Hash] = getAddressFromOutput(output: TransactionOutput) match {
         case Some(value) => Some(Hash(value))
          case None => None
        }

      val value = output.getValue.value

      if (outOfOrderInputMap.contains(transactionHash, index))
      {
        val (inputTxHash, blockOut) = outOfOrderInputMap(transactionHash, index)

        insertMovement(inputTxHash, transactionHash, addressOption, index, value, blockHeight, blockOut)

        outOfOrderInputMap -= (transactionHash -> index)
      }
      else
      {
        val address = addressOption match
        {
          case Some(address) => address
          case None =>          Hash.zero(0)
        }

        outputMap += ((transactionHash,index) -> (address, value, blockHeight))
      }

      totalOutIn += 1
      index += 1
    }
  }

  def saveBlock(b: Hash, txs: Int, btcs: Long, tstamp: Long) = {
    val height = longestChain.getOrElse(b,0)
    println("DONE: Saved block " + height)
    insertBlock(b, height, txs, btcs, tstamp)
  }

  def pre  = {
    outputMap = new UTXOs (immutable.HashMap[Hash,Hash]())
    outOfOrderInputMap = immutable.HashMap.empty
    vectorMovements = Vector()
    vectorBlocks = Vector()
    totalOutIn = 0
    println("DEBUG: Initiating database")
    initializeDB
  }

  def post = {
    saveUnmatchedOutputs
    saveUnmatchedInputs
    saveDataToDB

    println("DONE: " + totalOutIn + " movements, " + transactionCounter + " transactions saved in " + (System.currentTimeMillis - startTime)/1000 + "s")
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

  def saveUnmatchedInputs: Unit =
  {
    println(outOfOrderInputMap.size + " unmatched Inputs")
    //for (((outpointTransactionHash, outpointIndex), transactionHash) <- outOfOrderInputMap)
    //  insertInsertIntoList(Some(transactionHash), Some(outpointTransactionHash), None, Some(outpointIndex), None, None)

  }

  def saveDataToDB: Unit =
  {
    //println("DEBUG: Inserting data to database ...")

    if (vectorBlocks.length > 0)
    {
        val convertedVectorBlocks = vectorBlocks map { case (a,b,c,d,e) => (a.array.toArray,b,c,d,e) }
        blockDB.insertAll(convertedVectorBlocks:_*)
    }
    if (vectorMovements.length > 0)
    { def ohc(e:Option[Hash]):Array[Byte] = e.getOrElse(Hash.zero(0)).array.toArray

        def vectorMovementsConverter[A,B,C,D](v:Vector[(Hash,Hash,Option[Hash],A,B,C,D)]) = v map {
          case (a,b,c,d,e,f,g) => (Hash.hashToArray(a),Hash.hashToArray(b),ohc(c),d,e,f,g) }

        val convertedVectorMovements = vectorMovementsConverter(vectorMovements)

        movements.insertAll(convertedVectorMovements:_*)
      }


    vectorMovements = Vector()
    vectorBlocks = Vector()

    //println("DEBUG: Data inserted")
  }


// block
  def insertBlock(s: (Hash, Int, Int, Long, Long)) =
  {
    if (vectorMovements.length + vectorBlocks.length >= populateTransactionSize)
      saveDataToDB
    vectorBlocks +:= s
  }

  // movments
  def insertMovement(s: (Hash, Hash, Option[Hash], Int, Long, Int, Int)) =
  {
    if (vectorMovements.length + vectorBlocks.length >= populateTransactionSize)
      saveDataToDB

    vectorMovements +:= s
  }


  def includeInput(input: TransactionInput, transactionHash: Hash, blockOut: Int): Option[Hash] =
  {
    totalOutIn += 1

    val outpointTransactionHash = Hash(input.getOutpoint.getHash.getBytes)
    val outpointIndex = input.getOutpoint.getIndex.toInt

    if (outputMap.contains(outpointTransactionHash, outpointIndex))
    {
      val (address,value,blockIn) = outputMap(outpointTransactionHash, outpointIndex)

      insertMovement(
          transactionHash, outpointTransactionHash, Some(address), outpointIndex, value, blockIn, blockOut)

      outputMap -= (outpointTransactionHash -> outpointIndex)

      Some (address)
    }

    else
    {
      outOfOrderInputMap += ((outpointTransactionHash, outpointIndex) -> (transactionHash,blockOut))
      None
    }


  }


}
