package actions

import java.io.File

import core._
import util._
import util.Hash._

import scala.collection.immutable
import scalax.file.{Path,FileSystem}

import scala.slick.jdbc.meta._
import org.bitcoinj.core._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

trait FastBlockReader extends BlockReader
{
  // txhash -> ([address,...],[value,...]) (one entry per index)
  var outputMap: immutable.HashMap[Hash,(immutable.HashMap[Int,(Hash,Long)], Int)]  = immutable.HashMap()
  //  outpoint -> txhash
  var outOfOrderInputMap: immutable.HashMap[(Hash,Int),Hash]  = immutable.HashMap()
  var vectorMovements: Vector[(Option[Hash], Option[Hash], Option[Hash], Option[Int], Option[Long], Option[Int])] = Vector()
  var vectorBlocks: Vector[(Hash, Int, Int, Long, Long)]  = Vector()
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
      val addressOption: Option[Hash] = getAddressFromOutput(output: TransactionOutput) match {
          case Some(value) => Some(Hash(value))
          case None => None
        }
        
      //val addressOption: Option[Hash] = Some(Hash(getAddressFromOutput(output: TransactionOutput).getOrElse(None)))
      val value = output.getValue.value

      if (outOfOrderInputMap.contains(transactionHash, index))
      {
        val inputTxHash = outOfOrderInputMap(transactionHash, index)

        insertInsertIntoList(Some(inputTxHash), Some(transactionHash), addressOption, Some(index), Some(value), Some(blockHeight))
        //insertInsertIntoList(inputTxHash, transactionHash, addressOption, Some(index), Some(value), Some(blockHeight))
        outOfOrderInputMap -= (transactionHash -> index)
      }
      else
      {
        val address = addressOption match
        {
          case Some(address) => address
          case None =>          Hash.zero(0)
        }

        outputBuffer += (index -> (address, value))
      }

      totalOutIn += 1
      index += 1
    }

    if (!outputBuffer.isEmpty)
      outputMap += (transactionHash -> ((outputBuffer, blockHeight)))
  }

  def saveBlock(b: Hash, txs: Int, btcs: Long, tstamp: Long) = {
    val height = longestChain.getOrElse(b,0)
    println("DONE: Saved block " + height)
    insertInsertIntoList(b, height, txs, btcs, tstamp)
  }

  def pre  = {
    outputMap = immutable.HashMap()
    outOfOrderInputMap = immutable.HashMap()
    vectorMovements = Vector()
    vectorBlocks = Vector()
    totalOutIn = 0
    println("DEBUG: Initiating database")
    initializeDB
    (Q.u + "ALTER TABLE movements DISABLE KEYS;").execute
    (Q.u + "LOCK TABLES a WRITE;");
    //(Q.u+"SET GLOBAL max_allowed_packet=1073741824;").execute
    // (Q.u+"set global tmp_table_size = 1073741824;").execute
    // (Q.u+"set global max_heap_table_size = 1073741824;").execute
  }

  def post = {
    saveUnmatchedOutputs
    saveUnmatchedInputs
    saveDataToDB
    (Q.u + "ALTER TABLE movements ENABLE KEYS;").execute
    (Q.u + "UNLOCK TABLES;");

    println("DONE: " + totalOutIn + " movements, " + transactionCounter + " transactions saved in " + (System.currentTimeMillis - startTime)/1000 + "s")
  }

  def saveUnmatchedOutputs: Unit =
  {
    for ((transactionHash, (indexMap, blockHeight)) <- outputMap)
    {
      for ((index, (address, value)) <- indexMap)
        insertInsertIntoList(None, Some(transactionHash), Some(address), Some(index), Some(value),Some(blockHeight))
      outputMap -= transactionHash
    }
  }

  def saveUnmatchedInputs: Unit =
  {
    for (((outpointTransactionHash, outpointIndex), transactionHash) <- outOfOrderInputMap)
      insertInsertIntoList(Some(transactionHash), Some(outpointTransactionHash), None, Some(outpointIndex), None, None)

  }

  def saveDataToDB: Unit =
  {
    //println("DEBUG: Inserting data to database ...") 

    if (vectorBlocks.length > 0)
      (Q.u + "insert into blocks VALUES " + vectorBlocks.mkString(",")).execute
    if (vectorMovements.length > 0)
      { def ohc(e:Option[Hash]):Option[Array[Byte]] = for (h <- e) yield Hash.hashToArray(h)
                
        def vectorMovementsConverter[A,B,C](v:Vector[(Option[Hash],Option[Hash],Option[Hash],A,B,C)]) = v map {
          case (a,b,c,d,e,f) => (ohc(a),ohc(b),ohc(c),d,e,f) }

        val convertedVectorMovements = vectorMovementsConverter(vectorMovements)

        movements.insertAll(convertedVectorMovements:_*)
      }
     
    vectorMovements = Vector()
    vectorBlocks = Vector()
    //println("DEBUG: Data inserted")
  }

  def insertInsertIntoList(s: (Hash, Int, Int, Long, Long)) =
  {
    if (vectorMovements.length + vectorBlocks.length >= populateTransactionSize)
      saveDataToDB
    vectorBlocks +:= s
  }

  def insertInsertIntoList(s: (Option[Hash], Option[Hash], Option[Hash], Option[Int], Option[Long], Option[Int])) =
  {
    if (vectorMovements.length + vectorBlocks.length >= populateTransactionSize)
      saveDataToDB
//    val x = (s._1.getOrElse(Hash.zero(3)),s._2.getOrElse(Hash.zero(3)),s._3.getOrElse(Hash.zero(3)), s._4.getOrElse(0),s._5.getOrElse(0L),s._6.getOrElse(0))
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
          Some(transactionHash), Some(outpointTransactionHash), Some(outputTxMap(outpointIndex)._1), Some(outpointIndex), Some(outputTxMap(outpointIndex)._2), Some(blockHeight))
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
    deleteIfNotExists(stats, movements, blockDB, addresses, richestAddresses, richestClosures)
    stats.ddl.create
    movements.ddl.create
    blockDB.ddl.create
    addresses.ddl.create
    richestAddresses.ddl.create
    richestClosures.ddl.create
  }
} 
