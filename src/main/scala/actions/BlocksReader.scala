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
import scala.slick.driver.MySQLDriver.simple._
import scala.collection.JavaConversions._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import scala.collection._

class BlocksReader(args:List[String]){
  val params = MainNetParams.get()
  var start = 0
  var end = 0
  val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList());
  var totalOutIn = 0
  var vectorMovements:Vector[(Array[Byte], Array[Byte], Array[Byte], Int, Double)] = Vector()
  var vectorBlocks:Vector[Array[Byte]] = Vector()
  var readTime = System.currentTimeMillis;
  var outputMap: immutable.HashMap[Hash,(Array[Hash],Array[Double])] = immutable.HashMap() // txhash -> ([address,...],[value,...]) (one entry per index)
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
        "movements where spent_in_transaction_hash == " + Hash.zero(1).toString + " order by `index` desc ; "
    // now the highest remaining index in a transaction comes first
    println("Reading utxo Set")
    implicit val GetByteArr = GetResult(r => r.nextBytes)
    val q2 = Q.queryNA[(Array[Byte],Int,Array[Byte],Double)](query)

    for (quadruple <- q2) 
    {   
        val (hashArray,index,addressArray,value) = quadruple
        val hash = Hash(hashArray)
        val address = Hash(addressArray)
        
    	val (oldAddresses,oldValues):(Array[Hash], Array[Double]) = 
    	  if (outputMap.contains(hash)) 
    		outputMap(hash)
    	  else 
    	    (Array.fill(index+1)(Hash.zero(20)),Array.fill(index+1)(0))
    	val newValues = (oldAddresses.updated(index,address),oldValues.updated(index,value))
    	   
    	outputMap += (hash -> newValues)
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
    (Outputs.ddl).create
    (Blocks.ddl).create    
    (Addresses.ddl).create
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

    Blocks.insertAll(vectorBlocks: _*)
    Outputs.insertAll(vectorMovements: _*)

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

  def insertInsertIntoList(s: (Array[Byte], Array[Byte], Array[Byte], Int, Double)) =
  {
    if (vectorMovements.length + vectorBlocks.length >= populateTransactionSize) saveDataToDB

    vectorMovements +:= s
  }

  def wrapUpAndReturnTimeTaken(startTime: Long): Long =  	
  {
    for {(transactionHash, (addresses,values)) <- outputMap }
    {
 	 		for (i <- 0 until values.length )
      {
        if (values(i) != 0)
        {
          insertInsertIntoList((arrayNull, transactionHash.array.toArray,addresses(i).array.toArray,i,values(i)))
        }
      }
      outputMap -= transactionHash
    }
    
 	for (((outpointTransactionHash, outpointIndex), transactionHash) <- outOfOrderInputMap)
  {
    insertInsertIntoList((transactionHash.array.toArray, outpointTransactionHash.array.toArray, arrayNull, outpointIndex, 0.0))
  }
 	
    saveDataToDB     

    return System.currentTimeMillis - startTime  
  }

  def includeInput(input: TransactionInput, transactionHash: Hash) =
    {
      val outpointTransactionHash = Hash(input.getOutpoint.getHash.getBytes)
      val outpointIndex = input.getOutpoint.getIndex.toInt

      if (outputMap.contains(outpointTransactionHash))
      { 
    	  val outputTx = outputMap(outpointTransactionHash)
        insertInsertIntoList((transactionHash.array.toArray, outpointTransactionHash.array.toArray, outputTx._1(outpointIndex).array.toArray, outpointIndex, outputTx._2(outpointIndex)))
        outputTx._2(outpointIndex) = 0 // a value of 0 marks this output as spent
        if (outputTx._2.forall(_ == 0)) outputMap -= outpointTransactionHash
      } 
      else
        outOfOrderInputMap += ((outpointTransactionHash, outpointIndex) -> transactionHash)

      totalOutIn += 1
    }

  def getAddressFromOutput(output: TransactionOutput): Hash =
    Hash(try 
    {
      output.getScriptPubKey.getToAddress(params).getHash160
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
            address.getHash160
          } else { // special case because bitcoinJ doesn't support pay-to-IP scripts
            Array.fill(20)(0.toByte)
          }
        }
    })

  def includeTransaction(trans: Transaction) =
	{
    val transactionHash = Hash(trans.getHash.getBytes)

    if (!trans.isCoinBase)
    {
      for (input <- trans.getInputs)
        includeInput(input,transactionHash)
    }

    var index = 0
    val addressBuffer = scala.collection.mutable.ArrayBuffer.empty[Hash]
    val valueBuffer = collection.mutable.ArrayBuffer.empty[Double]

    for (output <- trans.getOutputs)
    {
      val address: Hash =
        try { getAddressFromOutput(output: TransactionOutput) }
        catch {
          case e: ScriptException =>
            println("bad transaction: " + transactionHash)
            Hash(Array.fill(20)(1.toByte))
        }

      addressBuffer += address
      val value = output.getValue.doubleValue

      if (outOfOrderInputMap.contains(transactionHash, index))
      {
        val inputTxHash = outOfOrderInputMap(transactionHash, index)
        insertInsertIntoList((inputTxHash.array.toArray, transactionHash.array.toArray, address.array.toArray, index, value))
        outOfOrderInputMap -= (transactionHash -> index)
        valueBuffer += 0
      }
      else
        valueBuffer += value

      totalOutIn += 1
      index += 1
    }

    if (!valueBuffer.forall(_ == 0))
      outputMap += (transactionHash -> (addressBuffer.toArray, valueBuffer.toArray))
  }
  
  def readBlocksfromFile: Long =
  {

    var savedBlocksSet:Set[Hash] = Set.empty
    val savedBlocks = for (b <- Blocks) yield (b.hash)
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
    (
      block <- asScalaIterator(loader)
      if (!savedBlocksSet.contains(Hash(block.getHashAsString())) && longestChain.contains(Hash(block.getHashAsString())))
    )
    {
      val blockHash = Hash(block.getHashAsString())
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

  var outputs: List[String] = List.empty

  transactionsDBSession
  {
    if (args.length > 1 && args(1) == "init" )  initializeDB
    else                                        blockCount = Query(Blocks.length).first
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
    //(Q.u + "create index if not exists address on movements (address)" + ";").execute
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