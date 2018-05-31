package core

//import Explorer._
import org.bitcoinj.core._
import scala.collection._
import scala.collection.JavaConversions._
import scala.slick.driver.PostgresDriver.simple._
import util._


// A FastBlockReader is a BlockReader that uses an UTXO set map
trait FastBlockReader extends BlockReader {

//  lazy val table = LmdbMap.create("utxos")
  lazy val table: mutable.Map[Hash, Hash] = mutable.Map.empty
  // (txhash,index) -> (address,value,blockIn)
  lazy val outputMap: UTXOs = new UTXOs (table)

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
      for {
        input <- inputsInTransaction(trans)
        addressOption = includeInput(input,transactionHash, blockHeight)
        if (addressOption != Some(Hash.zero(0)))
      }
      yield addressOption

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

  def finishBlock(b: Hash, txs: Int, btcs: Long, tstamp: Long, height:Int) = {
    processedBlocks :+= height
    insertBlock(b, height, txs, btcs, tstamp)
    //log.info("Saved block " + height + " consisting of " + txs + " txs")
  }

  def pre  = {
    outOfOrderInputMap = immutable.HashMap.empty
    vectorMovements = Vector()
    vectorBlocks = Vector()
    totalOutIn = 0
  }

  def post = {
    saveDataToDB
    saveUnmatchedInputs

    log.info("" + totalOutIn + " movements, " + transactionCounter + " transactions saved in " + (System.currentTimeMillis - startTime)/1000 + "s")
  }

  def saveUnmatchedInputs: Unit =
  {
    val unmatchedCount = outOfOrderInputMap.size
    for (input <- outOfOrderInputMap){
    	log.error(input.toString)
    }
    assert(unmatchedCount == 0, unmatchedCount + " unmatched Inputs found")
  }

  def saveDataToDB: Unit =
  {
    val amount = vectorBlocks.length + vectorMovements.length
    log.info("Saving blocks/movements (" + amount + ")  into database ...")

    val convertedVectorBlocks = vectorBlocks map { case (a,b,c,d,e) => (a.array.toArray,b,c,d,e) }
    DB.withSession(blockDB.insertAll(convertedVectorBlocks:_*)(_))

    def ohc(e:Option[Hash]):Array[Byte] = e.getOrElse(Hash.zero(0)).array.toArray
    def vectorMovementsConverter[A,B,C,D](v:Vector[(Hash,Hash,Option[Hash],A,B,C,D)]) = v map {
      case (a,b,c,d,e,f,g) => (Hash.hashToArray(a),Hash.hashToArray(b),ohc(c),d,e,f,g) }
    val convertedVectorMovements = vectorMovementsConverter(vectorMovements)

    try{
      DB.withSession(movements.insertAll(convertedVectorMovements:_*)(_))
    }
    catch {
      case e: java.sql.BatchUpdateException =>
        throw e.getNextException
    }


    vectorMovements = Vector()
    vectorBlocks = Vector()

    //log.info("Data inserted")
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
