package core

import org.bitcoinj.core._
import scala.collection._
import scala.collection.JavaConversions._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._

// A FastBlockReader is a BlockReader that uses an UTXO set map
abstract class FastBlockReader extends BlockReader {

  lazy val table = LmdbMap.create("utxos")
  // (txhash,index) -> (address,value,blockIn)
  lazy val outputMap: UTXOs = new UTXOs (table)

  //  outpoint -> (txhash,block_out)
  var outOfOrderInputMap: immutable.HashMap[(Hash,Int),(Hash,Int)]  = immutable.HashMap()
  var vectorMovements: Vector[(Hash, Hash, Option[Hash], Int, Long, Int, Int)] = Vector()
  var vectorBlocks: Vector[(Hash, Int, Int, Long, Long)]  = Vector()
//  lazy val  unmatchedClosure: mutable.HashMap[Hash,(Option[Hash],Int)] = mutable.HashMap() // spent_in_tx -> (Representant, count)
//  lazy val closures = new DisjointSets(new ClosureMap(LmdbMap.create("closures")))

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

    // TODO: put this direct closure stuff in an extra trait for testing purposes
    // ifnumberOfAddresses more than 1
    //   if all NONE
    //     unmatchedX += spent_in_tx -> (None, numberOfAddresses)
    //   else
    //     unmatchedX += spent_in_tx -> (first_Some, numberOfNones)
    //      for (x <- Somes) add(x)
    //       union (Somes)

    // val numberOfAddresses = addresses.size
    // if (numberOfAddresses > 1)
    // {
    //   val numberOfNones = addresses.count(_ == None)
    //   val firstAddress = addresses.find(_ != None)
    //   firstAddress match {
    //     case None =>
    //       unmatchedClosure += (transactionHash -> (None, numberOfAddresses))
    //     case (Some(someAddress)) =>
    //       unmatchedClosure += (transactionHash -> (someAddress, numberOfNones))
    //       // add and union all elements
    //       closures.union{
    //         for {
    //           someAddress <- addresses.filter(_ != None)
    //           address <- someAddress
    //         } yield {
    //           closures.add(address)
    //           address
    //         }
    //       }
    //   }
    // }

    var index = 0

    for (output <- outputsInTransaction(trans)) {
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



        // for {
        //   address <- addressOption
        //   (addOpt, count) <- unmatchedClosure.get(inputTxHash)
        // }
        // {
        //   closures add address

        //   for (add <- addOpt)
        //     closures.union(add, address)

        //   if (count == 1)
        //     unmatchedClosure -= inputTxHash
        //   else
        //     unmatchedClosure.update(inputTxHash, (Some(address), count-1))

        // }

        outOfOrderInputMap -= (transactionHash -> index)
      }
      else {
        val address = addressOption match
        {
          case Some(address) => address
          case None =>          Hash.zero(0)
        }

        addUTXO(blockHeight, transactionHash, index, value, address)
      }

      totalOutIn += 1
      index += 1
    }
  }

  def saveBlock(b: Hash, txs: Int, btcs: Long, tstamp: Long, height:Int) = {
    processedBlocks :+= height
    insertBlock(b, height, txs, btcs, tstamp)
    println("DEBUG: Saving block " + height + " consisting of " + txs + " txs at " + java.util.Calendar.getInstance().getTime() )
  }

  def pre  = {
    outOfOrderInputMap = immutable.HashMap.empty
    vectorMovements = Vector()
    vectorBlocks = Vector()
    totalOutIn = 0
  }

  def post = {
    saveUnmatchedInputs
    saveDataToDB

    println("DONE: " + totalOutIn + " movements, " + transactionCounter + " transactions saved in " + (System.currentTimeMillis - startTime)/1000 + "s")
  }

  def saveUnmatchedInputs: Unit =
  {
    assert(outOfOrderInputMap.size == 0, "unmatched Inputs")
    //for (((outpointTransactionHash, outpointIndex), transactionHash) <- outOfOrderInputMap)
    //  insertInsertIntoList(Some(transactionHash), Some(outpointTransactionHash), None, Some(outpointIndex), None, None)
  }

  def saveDataToDB: Unit =
  {
    val amount = vectorBlocks.length + vectorMovements.length
    println("DEBUG: Saving blocks/movements (" + amount + ")  into database ...")

    val convertedVectorBlocks = vectorBlocks map { case (a,b,c,d,e) => (a.array.toArray,b,c,d,e) }
    blockDB.insertAll(convertedVectorBlocks:_*)

    def ohc(e:Option[Hash]):Array[Byte] = e.getOrElse(Hash.zero(0)).array.toArray
    def vectorMovementsConverter[A,B,C,D](v:Vector[(Hash,Hash,Option[Hash],A,B,C,D)]) = v map {
      case (a,b,c,d,e,f,g) => (Hash.hashToArray(a),Hash.hashToArray(b),ohc(c),d,e,f,g) }
    val convertedVectorMovements = vectorMovementsConverter(vectorMovements)

    try{
      movements.insertAll(convertedVectorMovements:_*)
    }
    catch {
      case e: java.sql.BatchUpdateException =>
        throw e.getNextException
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

  // movements
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
      val (address, value, blockIn) = outputMap(outpointTransactionHash, outpointIndex)

      insertMovement(
          transactionHash, outpointTransactionHash, Some(address), outpointIndex, value, blockIn, blockOut)
      removeUTXO(outpointTransactionHash, outpointIndex)

      Some (address)
    }
    else {
      outOfOrderInputMap += ((outpointTransactionHash, outpointIndex) -> (transactionHash,blockOut))

      None
    }
  }

  // the following 2 methods have been factored out so we can extend their behavior in subclass ResumeBlockReader

  def addUTXO(blockHeight: Int, transactionHash: util.Hash, index: Int, value: Long, address: util.Hash): UTXOs = {
    outputMap += ((transactionHash,index) -> (address, value, blockHeight))
  }

  def removeUTXO(outpointTransactionHash: util.Hash, outpointIndex: Int): UTXOs = {
    outputMap -= (outpointTransactionHash -> outpointIndex)
  }


}
