/**
 * Created with IntelliJ IDEA.
 * User: stefan 
 * Date: 10/29/13
 * Time: 9:58 PM
 * To change this template use File | Settings | File Templates.
 */

import bddb._
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import BitcoinGraphExplorer.DisjointSetOfAddresses
import com.google.bitcoin.core._
import com.google.bitcoin.params.MainNetParams
import com.google.bitcoin.store.SPVBlockStore
import com.google.bitcoin.utils.BlockFileLoader
import java.io.File
import scala.collection.mutable.HashMap
import scala.slick.jdbc.meta.MTable
import scala.util.control.Exception
import scalax.io._
import scala.collection.JavaConversions._


object RawBlockFileReader extends App {

  val params = MainNetParams.get();

  val wallet = new Wallet(params)
  val blockStore = new SPVBlockStore(params, new File("bitcoin.blockchain2"));
  val chain = new BlockChain(params, wallet, blockStore)
  //val peers = new PeerGroup(params, chain)
  //val listener: DownloadListener = new Graph.NeoDownLoadListener(params)

  val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList());
  var counter = 0
  /////////////////
  var outputList:List[(Int, Int, Int, Double)] = Nil
  var inputList:List[(Int, Int, Int)] = Nil
  var blockList:List[(String,Int)] = Nil
  var transactionList:List[(String,Int,Int)] = Nil
  var allAddressesMap:HashMap[String,Int] = HashMap.empty
  var transactionsMap:HashMap[String,Int] = HashMap.empty
  var newAddressesMap:HashMap[String,Int] = HashMap.empty
  ///////////////

  var nrBlocksToSave = if (args.length > 0) args(0).toInt else 1000

  Database.forURL(
    url = "jdbc:mysql://localhost/bitcoin",
    driver = "com.mysql.jdbc.Driver",
    user = "root",
    password = "12345"

  )  withSession {

    var tableList = MTable.getTables.list;
    var tableMap = tableList.map{t => (t.name.name, t)}.toMap;

    if (args.length > 1 && args(1) == "init" )
    {

      if (tableMap.contains("b_outputs"))
        (Outputs.ddl).drop
      if (tableMap.contains("b_inputs"))
        (Inputs.ddl).drop
      if (tableMap.contains("b_blocks"))
        (Blocks.ddl).drop
      if (tableMap.contains("b_transactions"))
        (Transactions.ddl).drop
      if (tableMap.contains("b_addresses"))
        (Addresses.ddl).drop

    }

    tableList = MTable.getTables.list;
    tableMap = tableList.map{t => (t.name.name, t)}.toMap;


    if (!tableMap.contains("b_outputs"))
      (Outputs.ddl).create
    if (!tableMap.contains("b_inputs"))
      (Inputs.ddl).create
    if (!tableMap.contains("b_blocks"))
      (Blocks.ddl).create
    if (!tableMap.contains("b_transactions"))
      (Transactions.ddl).create
    if (!tableMap.contains("b_addresses"))
      (Addresses.ddl).create
    doSomethingBeautiful
    println("wir sind sehr geil!")



  }
  def hex2Bytes(hex: String): Array[Byte] = {
    (for {i <- 0 to hex.length - 1 by 2 if i > 0 || !hex.startsWith("0x")}
    yield hex.substring(i, i + 2))
      .map(Integer.parseInt(_, 16).toByte).toArray
  }
  def doSomethingBeautiful:Unit =
  {
    var savedBlocksSet:Set[String] = Set.empty
    val savedBlocks =
      (for (b <- Blocks /* if b.id === 42*/)
        yield (b.hash))

    for (c <- savedBlocks)
      savedBlocksSet = savedBlocksSet + c

    val savedAddresses =
      (for (a <- Addresses /* if b.id === 42*/)
      yield (a.hash,a.id))

    for (c <- savedAddresses)
      allAddressesMap += c

    val savedTransactions =
      (for (a <- Transactions /* if b.id === 42*/)
      yield (a.hash,a.id))

    for (c <- savedAddresses)
      transactionsMap += c


    var blockCount = Query(Blocks.length).first

    nrBlocksToSave += blockCount
    val saveInterval = 50000
    var transactionCount:Int = Query(Transactions.length).first
    var addressCount:Int = allAddressesMap.size
    println("Saving blocks from " + blockCount + " to " + nrBlocksToSave)

    for(
      block <- asScalaIterator(loader)
      if (!savedBlocksSet.contains(block.getHashAsString())))
    {
      val blockHash = block.getHashAsString()
      savedBlocksSet += blockHash
      if (counter > saveInterval || blockCount >= nrBlocksToSave ){
        val startTime = System.currentTimeMillis
        println("Saving until block nr. " + blockCount + " ...")
        Addresses.insertAll(newAddressesMap.toSeq:_*)
        Inputs.insertAll(inputList:_*)
        Outputs.insertAll(outputList:_*)
        Transactions.insertAll(transactionList:_*)
        Blocks.insertAll(blockList:_*)
        blockList = Nil
        transactionList = Nil
        outputList = Nil
        inputList = Nil
        allAddressesMap ++= newAddressesMap
        newAddressesMap = HashMap.empty
        counter = 0

        val totalTime = System.currentTimeMillis - startTime
        println("Saved in " + totalTime + "ms")
        if (blockCount >= nrBlocksToSave)
          return

      }


      blockCount += 1

      blockList = (blockHash, blockCount)::blockList

      for (trans <- block.getTransactions){
        transactionCount+=1
        val transactionHash = trans.getHashAsString()
        transactionList = (transactionHash, transactionCount, blockCount)::transactionList
        transactionsMap.update(transactionHash,transactionCount)

        if (!trans.isCoinBase)
        {
          for (input <- trans.getInputs)
          {
            val outpoint_transaction_hash = input.getOutpoint.getHash.toString
            val outpoint_transaction_id = transactionsMap.getOrElseUpdate(outpoint_transaction_hash,{transactionCount+=1;transactionCount})
            val outpoint_index = input.getOutpoint.getIndex.toInt
            inputList = (outpoint_transaction_id,outpoint_index, transactionCount)::inputList

            counter+=1
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

          val addressId = allAddressesMap.getOrElse(
            addressHash,
            {newAddressesMap.getOrElseUpdate(addressHash,{addressCount+=1;addressCount} )}
          )

          val value = output.getValue.doubleValue


          outputList = (transactionCount, addressId, index, value)::outputList
          //println(counter);
          counter+=1
          index+=1
        }
      }
    }
  }
}













