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
  lazy val table: LmdbMap = LmdbMap.create("utxos")
  lazy val outputMap: UTXOs = new UTXOs (table)


  //DBMaker.heapDB.transactionDisable.asyncWriteFlushDelay(100).make.getHashMap[(Hash,Int),(Hash,Long,Int)]("utxo")

  //  outpoint -> (txhash,block_out)
  var outOfOrderInputMap: immutable.HashMap[(Hash,Int),(Hash,Int)]  = immutable.HashMap()
  var vectorMovements: Vector[(Hash, Hash, Option[Hash], Int, Long, Int, Int)] = Vector()
  var vectorBlocks: Vector[(Hash, Int, Int, Long, Long)]  = Vector()
  lazy val  unmatchedClosure: mutable.HashMap[Hash,(Option[Hash],Int)] = mutable.HashMap() // spent_in_tx -> (Representant, count)
  lazy val closures = new DisjointSets(new ClosureMap(LmdbMap.create("closures")))

  var totalOutIn: Int = 0

  def useDatabase: Boolean = true

  def saveTransaction(trans: Transaction, blockHeight: Int) =
  {
    val transactionHash = Hash(trans.getHash.getBytes)

    val addresses =
      for {
        input <- inputsInTransaction(trans)
        addressOption = includeInput(input,transactionHash, blockHeight)
        if (addressOption != Some(Hash.zero(0)))
      }
      yield addressOption

    // ifnumberOfAddresses more than 1
    //   if all NONE
    //     unmatchedX += spent_in_tx -> (None, numberOfAddresses)
    //   else
    //     unmatchedX += spent_in_tx -> (first_Some, numberOfNones)
    //      for (x <- Somes) add(x)
    //       union (Somes)
    val numberOfAddresses = addresses.size
    if (numberOfAddresses > 1)
    {
      val numberOfNones = addresses.count(_ == None)
      val firstAddress = addresses.find(_ != None)
      firstAddress match {
        case None =>
          unmatchedClosure += (transactionHash -> (None, numberOfAddresses))
        case (Some(someAddress)) =>
          unmatchedClosure += (transactionHash -> (someAddress, numberOfNones))
          // add and union all elements
          closures.union{
            for {
              someAddress <- addresses.filter(_ != None)
              address <- someAddress
            } yield {
              closures.add(address)
              address
            }
          }
      }
    }

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

        // if (unmatchedX.contains(spent_in)
        //   add(address), dec
        //    case None => Some(address)
        //    case Some(e) => union(e, address)
        //  if int=0 delete



        for {
          address <- addressOption
          (addOpt, count) <- unmatchedClosure.get(inputTxHash)
        }
        {
          closures add address

          for (add <- addOpt)
            closures.union(add, address)

          if (count == 1)
            unmatchedClosure -= inputTxHash
          else
            unmatchedClosure.update(inputTxHash, (Some(address), count-1))

        }

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
    outOfOrderInputMap = immutable.HashMap.empty
    vectorMovements = Vector()
    vectorBlocks = Vector()
    totalOutIn = 0
    println("DEBUG: Initiating database")
    initializeDB
  }

  def post = {
    //saveUnmatchedOutputs
    saveUnmatchedInputs
    saveDataToDB
    table.commit
    // insert all elements from closures into the addresses table
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
