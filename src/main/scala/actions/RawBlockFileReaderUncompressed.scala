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
import scala.collection.mutable.HashMap
import scala.slick.jdbc.meta.MTable
import scala.collection.JavaConversions._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

class RawBlockFileReaderUncompressed(args:List[String]){
  val params = MainNetParams.get();
  var start = 0
  var end = 0
  val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList());
  var counter = 0
  var totalOutIn = 0
  var listData:List[String] = Nil
  var blockCount = 0
  var ad1Exists = false
  var ad2Exists = false
  // We need to capture these two fucking transactions because they are repeated.
  val ad1 = "d5d27987d2a3dfc724e359870c6644b40e497bdc0589a033220fe15429d88599"
  val ad2 = "e3bf3d07d4b0375638d5f1db5255fe07ba2c4cb067cd81b84ee974b6585fb468"
  var nrBlocksToSave = if (args.length > 0) args(0).toInt else 1000
  if (args.length > 1 && args(1) == "init" )   new File(db_file).delete



  def countInputs: Int =
  {
    Q.queryNA[Int]("""select count(*) from inputs""").list.head
  }

  def hex2Bytes(hex: String): Array[Byte] =
  {
    (for {i <- 0 to hex.length - 1 by 2 if i > 0 || !hex.startsWith("0x")}
      yield hex.substring(i, i + 2))
        .map(Integer.parseInt(_, 16).toByte).toArray
  }

  def initializeDB: Unit =
  {
    println("Resetting tables of the bitcoin database.")
    var tableList = MTable.getTables.list;
    var tableMap = tableList.map{t => (t.name.name, t)}.toMap;
    (RawOutputs.ddl).create
    (RawInputs.ddl).create
    (RawBlocks.ddl).create
    (Addresses.ddl).create
  }

  def saveDataToDB: Unit =
  {
    val startTime = System.currentTimeMillis
    println("Saving until block nr. " + blockCount + " ...")

    (Q.u + "BEGIN TRANSACTION").execute

    for (line <- listData)
      (Q.u + line+";").execute

    (Q.u + "COMMIT TRANSACTION").execute

    listData = Nil
    counter = 0
    val totalTime = System.currentTimeMillis - startTime
    println("Saved in " + totalTime + "ms")
  }

  def doSomethingBeautiful: Long =
  {
    println("Start")
    println("Reading binaries")
    var savedBlocksSet:Set[String] = Set.empty
    val savedBlocks =
      (for (b <- RawBlocks /* if b.id === 42*/)
        yield (b.hash))
    for (c <- savedBlocks)
      savedBlocksSet = savedBlocksSet + c

    nrBlocksToSave += blockCount
    println("Saving blocks from " + blockCount + " to " + nrBlocksToSave)
    val globalTime = System.currentTimeMillis
    for
    (
      block <- asScalaIterator(loader)
      if (!savedBlocksSet.contains(block.getHashAsString()))
    )
      {
      counter += 1

      val blockHash = block.getHashAsString()
      savedBlocksSet += blockHash

      if (counter > stepPopulate || blockCount >= nrBlocksToSave )
      {
        saveDataToDB

        if (blockCount >= nrBlocksToSave)
          return System.currentTimeMillis - globalTime

      }
      blockCount += 1
      listData = "insert into blocks VALUES (" + '"' + blockHash + '"' + ")"::listData

      for (trans <- block.getTransactions)
      {
        val transactionHash = trans.getHashAsString

        if (!trans.isCoinBase)
        {
          for (input <- trans.getInputs)
          {
            val outpointTransactionHash = input.getOutpoint.getHash.toString
            val outpointIndex = input.getOutpoint.getIndex.toInt
            listData = "insert into inputs (output_transaction_hash, output_index, transaction_hash) VALUES (" + '"' + outpointTransactionHash + '"' + "," + outpointIndex +"," + '"' + transactionHash+'"'+")"::listData
            counter+=1
            totalOutIn+=1
          }
        }
        var index = 0

        for (output <- trans.getOutputs)
        {
          val addressHash:String =
            try
            {
              output.getScriptPubKey.getToAddress(params).toString
            }
            catch
            {
              case e: ScriptException =>
                try
                {
	                val script = output.getScriptPubKey.toString
	                //TODO: 
	                // can we generate an address for pay-to-ip?
	                
	                if (script.startsWith("[65]"))
	                {
	                  val pubkeystring = script.substring(4, 134)
	                  import Utils._
	                  val pubkey = hex2Bytes(pubkeystring)
	                  val address = new Address(params, sha256hash160(pubkey))
	                  address.toString
	                }
	                else
	                { // special case because bitcoinJ doesn't support pay-to-IP scripts
	                  "0"
	                }
                }
                catch
                {
                  case e: ScriptException =>
                  	println("bad transaction: "+transactionHash)
                  	"dead"
                }
            }
          val value = output.getValue.doubleValue
          if ( (transactionHash != ad1 || !ad1Exists) && (transactionHash != ad2 || !ad2Exists))
          {
            listData = "insert into outputs (transaction_hash, address, `index`, `value`) VALUES (" + '"' + transactionHash + '"' + "," + '"'+addressHash + '"' + "," + index + "," + value + ")"::listData
            counter+=1
            totalOutIn+=1
            index+=1
            ad1Exists = ad1Exists || (transactionHash == ad1)
            ad2Exists = ad2Exists || (transactionHash == ad2)
          }
        }
      }
    }
    return 0.toLong
  }

  databaseSession
  {

    if (args.length > 1 && args(1) == "init" )
    {
      initializeDB
    }

    else
    {
      blockCount = Query(RawBlocks.length).first
    }

    if (Q.queryNA[Int]("""select count(*) from outputs where transaction_hash = """"+ad1+"""";""").list.head == 1)
      ad1Exists = true
    if (Q.queryNA[Int]("""select count(*) from outputs where transaction_hash = """"+ad2+"""";""").list.head == 1)
      ad2Exists = true

    //(Q.u + "PRAGMA foreign_keys=OFF;").execute
    start = countInputs
    val totalTime = doSomethingBeautiful
    end = countInputs
    //(Q.u + "PRAGMA foreign_keys=ON;").execute

    println("Total time to save movements = " + totalTime + " ms")
    println("Total of movements = " + totalOutIn)
    println("Time required pro movement = " + totalTime.toDouble/totalOutIn +" ms")
    println("Wir sind sehr geil!")
  }
}