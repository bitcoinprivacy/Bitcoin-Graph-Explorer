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
import com.google.bitcoin.store.SPVBlockStore
import com.google.bitcoin.utils.BlockFileLoader
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import scala.slick.jdbc.meta.MTable
import scala.collection.JavaConversions._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import scala.collection._
import javax.sql.rowset.serial.SerialBlob

class BlocksReader(args:List[String]){
  val params = MainNetParams.get();
  var start = 0
  var end = 0
  val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList());
  var totalOutIn = 0
  var listData:Vector[String] = Vector()
  var readTime = System.currentTimeMillis;
  val outputMap: mutable.HashMap[Hash,(Array[Hash],Array[Double])] = mutable.HashMap() // txhash -> ([address,...],[value,...]) (one entry per index)
  val outOfOrderInputMap: mutable.HashMap[(Hash,Int),Hash] = mutable.HashMap() //  outpoint -> txhash
  var blockCount = 0
  var ad1Exists = false
  var ad2Exists = false
  // We need to capture these two transactions because they are repeated.
  val duplicatedTx1 = Hash("d5d27987d2a3dfc724e359870c6644b40e497bdc0589a033220fe15429d88599")
  val duplicatedTx2 = Hash("e3bf3d07d4b0375638d5f1db5255fe07ba2c4cb067cd81b84ee974b6585fb468")
  				 
  
  var nrBlocksToSave = if (args.length > 0) args(0).toInt else 1000
  if (args.length > 1 && args(1) == "init" )   new File(databaseFile).delete


  def populateOutputMap = 
  {
    val query = """ select transaction_hash, `index`, address, `value` from
        movements where spent_in_transaction_hash IS NULL order by `index` desc ; """
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
    val query = """ select spent_in_transaction_hash, transaction_hash, `index` from
        movements where address IS NULL; """
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
    println("Resetting tables of the bitcoin database.")
    (Outputs.ddl).create
    (RawBlocks.ddl).create
    (Addresses.ddl).create
  }

  def saveDataToDB: Unit =
  {
    val startTime = System.currentTimeMillis
    val timeUntilLastSave = startTime - readTime ;

    println(
      """     Read in """ + timeUntilLastSave + """ ms
       Blocks read """ + blockCount + """
       SQL transaction size: """ + listData.size + """
       Outputs in memory: """ + outputMap.size + """
       Inputs in memory: """ + outOfOrderInputMap.size + """
       Saving blocks ... """
    )


    (Q.u + "BEGIN TRANSACTION").execute

    for (line <- listData)
    {
      (Q.u + line+";").execute
    }
    (Q.u + "COMMIT TRANSACTION").execute

    listData = Vector()
    val totalTime = System.currentTimeMillis - startTime
    println(
      """     Saved in """ + totalTime + """ ms
=============================================
       Reading blocks ..."""
    )
    readTime = System.currentTimeMillis
  }

  def insertInsertIntoList(s:String) =
  {
    if (listData.length >= stepPopulate) saveDataToDB

    listData +:= s
  }

  def wrapUpAndReturnTimeTaken(startTime: Long): Long =  	
  {
    for {(transactionHash, (addresses,values)) <- outputMap }
    {
 	 		for (i <- 0 until values.length )
      {
        if (values(i) != 0)
        {
          insertInsertIntoList("INSERT INTO movements (transaction_hash, `index`, address, `value`) VALUES " +
    					" ("+ transactionHash + ", "+ i + ", " + addresses(i) + ", "+ values(i) +")")
        }
      }
      outputMap -= transactionHash
    }
    
 	for (((outpointTransactionHash, outpointIndex), transactionHash) <- outOfOrderInputMap)
    {
      insertInsertIntoList("INSERT INTO movements (spent_in_transaction_hash, transaction_hash, `index`) VALUES " +
   					" ("+ transactionHash + ", "+ outpointTransactionHash + ", "+ outpointIndex +")")
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
    	if (outpointIndex >= outputTx._1.length)
    	{  println(outputTx._1.deep)
    	  println(outpointIndex)
    	  println(outpointTransactionHash)
    	}  
    	if (outpointIndex >= outputTx._2.length)
    	{  println(outputTx._2.deep)
    	  println(outpointIndex)
    	}
        insertInsertIntoList("INSERT INTO movements (spent_in_transaction_hash, transaction_hash, `index`, address, `value`) VALUES " +
          " (" + transactionHash + ", " + outpointTransactionHash + ", " + outpointIndex + ", " + outputTx._1(outpointIndex) + ", " + outputTx._2(outpointIndex) + ")")
        outputTx._2(outpointIndex) = 0 // a value of 0 marks this output as spent

        if (outputTx._2.forall(_ == 0))
          outputMap -= outpointTransactionHash
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
            val pubkey = Hash(pubkeystring).array
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
          insertInsertIntoList("INSERT INTO movements (spent_in_transaction_hash, transaction_hash, `index`, address, `value`) VALUES " +
            " (" + inputTxHash + ", " + transactionHash + ", " + index + ", " + address+ ", " + value + "')")
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
  
  def processBlockHash: Long =
  { 
    println("Reading binaries")
    var savedBlocksSet:Set[String] = Set.empty
    val savedBlocks =
      for (b <- RawBlocks)
        yield (b.hash)
    for (c <- savedBlocks)
      savedBlocksSet = savedBlocksSet + c

    nrBlocksToSave += blockCount

    val startTime = System.currentTimeMillis
    
    populateOOOInputMap
    populateOutputMap

    println("Saving blocks from %s to %s" format (blockCount, nrBlocksToSave))
    println("""=============================================
       Reading blocks ..."""
    )

    for
    (
      block <- asScalaIterator(loader)
      if (!savedBlocksSet.contains(block.getHashAsString()))
    )
    {

      val blockHash = block.getHashAsString()
      savedBlocksSet += blockHash

      if ( blockCount >= nrBlocksToSave )
      {
        return wrapUpAndReturnTimeTaken(startTime)
      }

      blockCount += 1

      insertInsertIntoList("insert into blocks VALUES (" + '"' + blockHash + '"' + ")")

      for (trans <- block.getTransactions) 
      { 
        val transactionHash = Hash(trans.getHash.getBytes)
        if ((transactionHash != duplicatedTx1 || !ad1Exists) && (transactionHash != duplicatedTx2 || !ad2Exists))
        {
    	  includeTransaction(trans)
    	  ad1Exists = ad1Exists || (transactionHash == duplicatedTx1)
          ad2Exists = ad2Exists || (transactionHash == duplicatedTx2)
        }
      }
    }
    return wrapUpAndReturnTimeTaken(startTime)
  }
  
  databaseSession
  {

    if (args.length > 1 && args(1) == "init" )
    {
      initializeDB
      (Q.u + "analyze;"                         ).execute
    }
    else
    {
      blockCount = Query(RawBlocks.length).first
    }


    if (Q.queryNA[Int]("select count(*) from movements where transaction_hash = "+duplicatedTx1+";").list.head == 1)
      ad1Exists = true
    if (Q.queryNA[Int]("select count(*) from movements where transaction_hash = "+duplicatedTx2+";").list.head == 1)
      ad2Exists = true


    start = countInputs
    val totalTime = processBlockHash
    end = countInputs
    //(Q.u + "PRAGMA foreign_keys=ON;").execute
    println("     Blocks processed!")
    println("=============================================")
    println()
    println("/////////////////////////////////////////////")
    println("Total time to save movements = " + totalTime + " ms")
    println("Total of movements = " + totalOutIn)
    println("Time required pro movement = " + totalTime.toDouble/totalOutIn +" ms")
    println("/////////////////////////////////////////////")
    println()
  }
}