package actions

/**
 * Created with IntelliJ IDEA.
 * User: stefan 
 * Date: 10/29/13
 * Time: 9:58 PM
 * To change this template use File | Settings | File Templates.
 */

import libs._
import java.io._
import com.google.bitcoin.core._
import com.google.bitcoin.params.MainNetParams
import com.google.bitcoin.utils.BlockFileLoader
import scala.slick.driver.SQLiteDriver.simple._
import scala.collection.JavaConversions._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
// TODO: find out about static vs dynamic sessions
import scala.collection._

class BlocksReader(args:List[String]){
  val params = MainNetParams.get()
  var start = 0
  var end = 0
  val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList());
  var totalOutIn = 0
  var vectorMovements:
	  Vector[(Option[Array[Byte]], Option[Array[Byte]], Option[Array[Byte]], Option[Int], Option[Double])] = Vector()
  var vectorBlocks:Vector[Array[Byte]] = Vector()
  var readTime = System.currentTimeMillis;
  var outputMap: immutable.HashMap[Hash,immutable.HashMap[Int,(Hash,Double)]] = immutable.HashMap() // txhash -> ([address,...],[value,...]) (one entry per index)
  var outOfOrderInputMap: immutable.HashMap[(Hash,Int),Hash] = immutable.HashMap() //  outpoint -> txhash
  var blockCount = 0
  val longestChain = getLongestBlockChainHashSet
  // We need to watch out for these two transactions because they are repeated in the blockchain.
  val duplicatedTx1 = Hash("d5d27987d2a3dfc724e359870c6644b40e497bdc0589a033220fe15429d88599")
  val duplicatedTx2 = Hash("e3bf3d07d4b0375638d5f1db5255fe07ba2c4cb067cd81b84ee974b6585fb468")
  var duplicatedTx1Exists = false
  var duplicatedTx2Exists = false				 
  
  var nrBlocksToSave = if (args.length > 0) args(0).toInt else 1000
  if (args.length > 1 && args(1) == "init" )   new File(transactionsDatabaseFile).delete


  def populateOutputMap = 
  {
    val query = " select transaction_hash, `index`, address, `value` from " +
        "movements where spent_in_transaction_hash IS NULL; "
   
    println("Reading utxo Set")
    implicit val GetByteArr = GetResult(r => r.nextBytes)
    val q2 = Q.queryNA[(Array[Byte],Int,Array[Byte],Double)](query)

    for (quadruple <- q2) 
    {   
        val (hashArray,index,addressArray,value) = quadruple
        val hash = Hash(hashArray)
        val address = Hash(addressArray)
        
    	val oldMap = outputMap.getOrElse(hash,immutable.HashMap())
    	  
    	val newMap = oldMap + (index -> (address,value))
    	   
    	outputMap += (hash -> newMap)
    }
  }  
    
  def populateOOOInputMap =
  {
    val query = " select spent_in_transaction_hash, transaction_hash, `index` from " +
        " movements where address == " + Hash.zero(1).toString
    println("Reading Out-Of-Order Input Set")
    implicit val GetByteArr = GetResult(r => r.nextBytes)
    val q2 = Q.queryNA[(Array[Byte],Array[Byte],Int)](query)
    
    for (triple <- q2)
    {
      val (spentTx,hash,index) = triple
      outOfOrderInputMap += ((Hash(hash),index) -> Hash(spentTx))
    }  
  }  
    
  def initializeDB: Unit =
  {
    outputs.ddl.create
    blocks.ddl.create    
    addresses.ddl.create
  }

  def saveDataToDB: Unit =
  {
    val startTime = System.currentTimeMillis
    val timeUntilLastSave = startTime - readTime ;

    println("     Read in " + timeUntilLastSave + " ms"          )
    println("     Blocks read " + blockCount                     )
    println("     SQL transaction size: " + vectorMovements.size )
    println("     Outputs in memory: " + outputMap.size          )
    println("     Inputs in memory: " + outOfOrderInputMap.size  )
    println("     Saving blocks ..."                             )

    blocks.insertAll(vectorBlocks: _*)
    outputs.insertAll(vectorMovements: _*)

    vectorMovements = Vector()
    vectorBlocks = Vector()

    val totalTime = System.currentTimeMillis - startTime
    println("     Saved in " + totalTime + " ms"           )
    println("=============================================")
    println("     Reading blocks ..."                      )

    readTime = System.currentTimeMillis
  }

  def insertInsertIntoList(s: Array[Byte]) =
  {
    if (vectorMovements.length + vectorBlocks.length >= populateTransactionSize) saveDataToDB

    vectorBlocks +:= s
  }

  def insertInsertIntoList
  	(s: (Option[Array[Byte]], Option[Array[Byte]], Option[Array[Byte]], Option[Int], Option[Double])) =
  {
    if (vectorMovements.length + vectorBlocks.length >= populateTransactionSize) saveDataToDB

    vectorMovements +:= s
  }

  def wrapUpAndReturnTimeTaken(startTime: Long): Long =  	
  {
    for ((transactionHash, indexMap) <- outputMap)
      {
      for ((index, (address, value)) <- indexMap)
    	  insertInsertIntoList((None, transactionHash.toSomeArray, address.toSomeArray, Some(index), Some(value)))       
      outputMap -= transactionHash
      }
    
   for (((outpointTransactionHash, outpointIndex), transactionHash) <- outOfOrderInputMap)
      insertInsertIntoList((transactionHash.toSomeArray, outpointTransactionHash.toSomeArray, None, Some(outpointIndex), None))
    
 	
    saveDataToDB     

    return System.currentTimeMillis - startTime  
  }

  def includeInput(input: TransactionInput, transactionHash: Hash) =
    {
      val outpointTransactionHash = Hash(input.getOutpoint.getHash.getBytes)
      val outpointIndex = input.getOutpoint.getIndex.toInt

      if (outputMap.contains(outpointTransactionHash) && outputMap(outpointTransactionHash).contains(outpointIndex))
      { 
    	val outputTxMap = outputMap(outpointTransactionHash)
        insertInsertIntoList(
            (transactionHash.toSomeArray, outpointTransactionHash.toSomeArray, outputTxMap(outpointIndex)._1.toSomeArray, Some(outpointIndex), Some(outputTxMap(outpointIndex)._2)))
        val updatedTxMap = outputTxMap - outpointIndex
        if (updatedTxMap.isEmpty) 
          outputMap -= outpointTransactionHash
        else
          outputMap += (outpointTransactionHash -> updatedTxMap)
      } 
      else
        outOfOrderInputMap += ((outpointTransactionHash, outpointIndex) -> transactionHash)

      totalOutIn += 1
    }

  def getAddressFromOutput(output: TransactionOutput): Option[Array[Byte]] =
    try
    {
      Some(output.getScriptPubKey.getToAddress(params).getHash160)
    } 
    catch 
    {
      case e: ScriptException =>
        try 
        {
          val script = output.getScriptPubKey.toString
          //TODO: 
          // can we generate an address for pay-to-ip?

          if (script.startsWith("[65]")) {
            val pubkeystring = script.substring(4, 134)
            import Utils._
            val pubkey = Hash(pubkeystring).array.toArray
            val address = new Address(params, sha256hash160(pubkey))
            Some(address.getHash160)
          } else { // special case because bitcoinJ doesn't support pay-to-IP scripts
            None
          }
        }
        catch {
          case e: ScriptException =>
            println("bad transaction output: " + output)
            None
        }
      
    }

  def includeTransaction(trans: Transaction) =
	{
    val transactionHash = Hash(trans.getHash.getBytes)

    if (!trans.isCoinBase)
    {
      for (input <- trans.getInputs)
        includeInput(input,transactionHash)
    }

    var index = 0
    var outputBuffer = immutable.HashMap[Int,(Hash,Double)]()

    for (output <- trans.getOutputs)
    {
      val addressOption: Option[Array[Byte]] = getAddressFromOutput(output: TransactionOutput) 
      val value = output.getValue.doubleValue
      
      for (address <- addressOption)
      {
      val value = output.getValue.doubleValue

      if (outOfOrderInputMap.contains(transactionHash, index))
      {
        val inputTxHash = outOfOrderInputMap(transactionHash, index)
        insertInsertIntoList(
            (inputTxHash.toSomeArray, transactionHash.toSomeArray, addressOption, Some(index), Some(value)))
        outOfOrderInputMap -= (transactionHash -> index)
      }
      else
        outputBuffer += (index -> (Hash(address), value))
      }

      totalOutIn += 1
      index += 1
    }

    if (!outputBuffer.isEmpty)
      outputMap += (transactionHash -> outputBuffer)
  }
  
  def readBlocksfromFile: Long =
  {
    var savedBlocksSet:Set[Hash] = Set.empty
    val savedBlocks = for (b <- blocks) yield (b.hash)
    for (c <- savedBlocks) savedBlocksSet = savedBlocksSet + Hash(c)
    nrBlocksToSave += blockCount
    startLogger("populate_"+blockCount+"_"+nrBlocksToSave)
    val startTime = System.currentTimeMillis
    println("Reading binaries")

    populateOOOInputMap
    populateOutputMap

    println("Saving blocks from %s to %s" format (blockCount, nrBlocksToSave))
    println("""=============================================
       Reading blocks ..."""
    )

    for
    {
      block <- asScalaIterator(loader)
      val blockHash = Hash(block.getHash.getBytes)
      if (!savedBlocksSet.contains(blockHash) && longestChain.contains(blockHash))
    }
    {
      // TODO: fix populate over existing data, there is a not unique error!s

      savedBlocksSet += blockHash

      if ( blockCount >= nrBlocksToSave )   return wrapUpAndReturnTimeTaken(startTime)

      blockCount += 1
      insertInsertIntoList(blockHash.array.toArray)

      for (trans <- block.getTransactions) 
      { 
        val transactionHash = Hash(trans.getHash.getBytes)
        if ((transactionHash != duplicatedTx1 || !duplicatedTx1Exists) &&
            (transactionHash != duplicatedTx2 || !duplicatedTx2Exists))
        {
    	  includeTransaction(trans)
    	  duplicatedTx1Exists = duplicatedTx1Exists || (transactionHash == duplicatedTx1)
          duplicatedTx2Exists = duplicatedTx2Exists || (transactionHash == duplicatedTx2)
        }
      }
    }

    return wrapUpAndReturnTimeTaken(startTime)
  }

  transactionsDBSession
  {
    if (args.length > 1 && args(1) == "init" )  initializeDB
    else blockCount = blocks.size.asInstanceOf[Int]
    if (Q.queryNA[Int]("select count(*) from movements where transaction_hash = "+duplicatedTx1+";").list.head == 1)
      duplicatedTx1Exists = true
    if (Q.queryNA[Int]("select count(*) from movements where transaction_hash = "+duplicatedTx2+";").list.head == 1)
      duplicatedTx2Exists = true
    start = countInputs
    val totalTime = readBlocksfromFile
    end = countInputs
    println("     Blocks processed!")
    println("=============================================")
    println("     Creating indexes ...")
    var clockIndex = System.currentTimeMillis
    (Q.u + "create index if not exists address on movements (address)" + ";").execute
    (Q.u + "create unique index if not exists transaction_hash_i on movements (transaction_hash, `index`)" + ";").execute
    (Q.u + "create index if not exists spent_in_transaction_hash on movements (spent_in_transaction_hash)" + ";").execute
    println("     Indices created")
    println("=============================================")
    println("")
    clockIndex = System.currentTimeMillis - clockIndex
    println("/////////////////////////////////////////////")
    println("Indexes created in %s" format (clockIndex/1000))
    println("Total time to save movements %s s" format (totalTime/1000))
    println("Total of movements = %s" format (totalOutIn))
    println("Time required pro movement %s µs " format (1000 * totalTime/totalOutIn))
    println("/////////////////////////////////////////////")
  }
}
