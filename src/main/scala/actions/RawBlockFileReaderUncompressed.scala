package actions

/**
 * Created with IntelliJ IDEA.
 * User: stefan 
 * Date: 10/29/13
 * Time: 9:58 PM
 * To change this template use File | Settings | File Templates.
 */

import libs._
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import com.google.bitcoin.core._
import com.google.bitcoin.params.MainNetParams
import com.google.bitcoin.store.SPVBlockStore
import com.google.bitcoin.utils.BlockFileLoader
import scala.collection.mutable.HashMap
import scala.slick.jdbc.meta.MTable
import scala.collection.JavaConversions._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

class RawBlockFileReaderUncompressed(args:List[String]){
  val params = MainNetParams.get();
  val wallet = new Wallet(params)
  val blockStore = new SPVBlockStore(params, new java.io.File("bitcoin.blockchain"));
  val chain = new BlockChain(params, wallet, blockStore)
  val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList());
  var counter = 0
  var totalOutIn = 0
  /////////////////
  var outputList:List[(String, String, Int, Double)] = Nil
  var inputList:List[(String, Int, String)] = Nil
  var blockList:List[(String)] = Nil
  ///////////////
  var nrBlocksToSave = if (args.length > 0) args(0).toInt else 1000
  databaseSession {
    var tableList = MTable.getTables.list;
    var tableMap = tableList.map{t => (t.name.name, t)}.toMap;
    if (args.length > 1 && args(1) == "init" )
    {
      println("Resetting tables from the bitcoins database.")
      if (tableMap.contains("outputs"))
        (RawOutputs.ddl).drop
      (RawOutputs.ddl).create
      if (tableMap.contains("inputs"))
        (RawInputs.ddl).drop
      (RawInputs.ddl).create
      if (tableMap.contains("blocks"))
        (RawBlocks.ddl).drop
      (RawBlocks.ddl).create
    }
    tableList = MTable.getTables.list;
    tableMap = tableList.map{t => (t.name.name, t)}.toMap;
    val totalTime = doSomethingBeautiful
    Q.queryNA[String]("unlock tables")
    // Remove the duplicate fucking bugged entries


    println("Total time to save movements = " + totalTime + " ms")
    println("Total of movements = " + totalOutIn)
    println("Time required pro movement = " + totalTime.toDouble/totalOutIn +" ms")
    println("Wir sind sehr geil!")
  }
  def hex2Bytes(hex: String): Array[Byte] = {
    (for {i <- 0 to hex.length - 1 by 2 if i > 0 || !hex.startsWith("0x")}
      yield hex.substring(i, i + 2))
        .map(Integer.parseInt(_, 16).toByte).toArray
  }
  def doSomethingBeautiful: Long =
  {

    Q.queryNA[String]("""lock tables `blocks` WRITE""")
    Q.queryNA[String]("lock tables `inputs` WRITE")
    Q.queryNA[String]("lock tables `outputs` WRITE")
    var savedBlocksSet:Set[String] = Set.empty
    val savedBlocks =
      (for (b <- RawBlocks /* if b.id === 42*/)
        yield (b.hash))
    for (c <- savedBlocks)
      savedBlocksSet = savedBlocksSet + c
    var blockCount = Query(RawBlocks.length).first
    nrBlocksToSave += blockCount
    val saveInterval = 100000
    println("Saving blocks from " + blockCount + " to " + nrBlocksToSave)
    val globalTime = System.currentTimeMillis

    for(
      block <- asScalaIterator(loader)
      if (!savedBlocksSet.contains(block.getHashAsString())))
    {
      val blockHash = block.getHashAsString()
      savedBlocksSet += blockHash
      if (counter > saveInterval || blockCount >= nrBlocksToSave ){
        val startTime = System.currentTimeMillis
        println("Saving until block nr. " + blockCount + " ...")

        (Q.u + """insert into inputs values """+
          inputList.toString.drop(5).dropRight(1)+""";""").execute
        //RawInputs.insertAll(inputList:_*)
        (Q.u + """insert into outputs values """+
          outputList.toString.drop(5).dropRight(1)+""";""").execute
        //RawOutputs.insertAll(outputList:_*)
        (Q.u + """insert into blocks values """+
          blockList.toString.drop(5).dropRight(1)+""";""").execute
        //RawBlocks.insertAll(blockList:_*)
        blockList = Nil
        outputList = Nil
        inputList = Nil
        counter = 0
        val totalTime = System.currentTimeMillis - startTime
        println("Saved in " + totalTime + "ms")
        if (blockCount >= nrBlocksToSave)
        {
          return System.currentTimeMillis - globalTime

        }
      }
      blockCount += 1
      blockList = """(""""+blockHash+"""")"""::blockList
      for (trans <- block.getTransactions){
        val transactionHash = trans.getHashAsString()
        if (!trans.isCoinBase)
        {
          for (input <- trans.getInputs)
          {
            val outpointTransactionHash = input.getOutpoint.getHash.toString
            val outpointIndex = input.getOutpoint.getIndex.toInt
            inputList = ('"'+outpointTransactionHash+'"',outpointIndex, '"'+transactionHash+'"')::inputList
            counter+=1
            totalOutIn+=1
          }
        }
        var index = 0
        for (output <- trans.getOutputs)
        {
          val addressHash:String =
            try {
              output.getScriptPubKey.getToAddress(params).toString
            }
            catch {
              case e: ScriptException =>
                val script = output.getScriptPubKey.toString
                if (script.startsWith("[65]")) {
                  val pubkeystring = script.substring(4, 134)
                  import Utils._
                  val pubkey = hex2Bytes(pubkeystring)
                  val address = new Address(params, sha256hash160(pubkey))
                  address.toString
                }
                // special case because bitcoinJ doesn't support pay-to-IP scripts
                else {
                  //println("can't parse script: " + output.getScriptPubKey.toString)
                  "0"
                }
            }
          val value = output.getValue.doubleValue
          outputList = ('"'+transactionHash+'"', '"'+addressHash+'"', index, value)::outputList
          counter+=1
          totalOutIn+=1
          index+=1
        }
      }
    }
    return 0.toLong
  }
}