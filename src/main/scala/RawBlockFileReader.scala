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
  var outputList:List[(Int, Int, Int, Int)] = Nil
  //var inputList:List[(String, Long, Int, String, String, Long)] = List.empty
  var blockList:List[(String,Int)] = Nil
  var transactionList:List[(String,Int,Int)] = Nil
  var addressMap:HashMap[String,Int] = HashMap.empty
  ///////////////

  val nrBlocksToSave = if (args.length > 0) args(0).toInt else Int.MaxValue

  Database.forURL(
    url = "jdbc:mysql://localhost/bitcoin",
    driver = "com.mysql.jdbc.Driver",
    user = "root",
    password = "12345"

  )  withSession {
    (Outputs.ddl ++ Inputs.ddl ++ Blocks.ddl ++ Transactions.ddl ++ Addresses.ddl).drop
    (Outputs.ddl ++ Inputs.ddl ++ Blocks.ddl ++ Transactions.ddl ++ Addresses.ddl).create
    doSomethingBeautiful
    println("wir sind geil!")



  }
  def hex2Bytes(hex: String): Array[Byte] = {
    (for {i <- 0 to hex.length - 1 by 2 if i > 0 || !hex.startsWith("0x")}
    yield hex.substring(i, i + 2))
      .map(Integer.parseInt(_, 16).toByte).toArray
  }
  def doSomethingBeautiful:Unit =
  {
var blockCount = Query(Blocks.length).first
    var transactionCount:Int = Query(Transactions.length).first
    var addressCount:Int = Query(Addresses.length).first
    println("Saving blocks from " + blockCount + " to " + nrBlocksToSave)
    for (block <- asScalaIterator(loader) )
    //   if (Blocks.findByHash(block.getHashAsString())).isEmpty
    {
      if (blockCount > nrBlocksToSave)
      {
        Addresses.insertAll(addressMap.toSeq:_*)

        return
      }




      val blockHash = block.getHashAsString();
      if (counter > 50000){
        val startTime = System.currentTimeMillis
        //Inputs.insertAll(inputList:_*)
        Outputs.insertAll(outputList:_*)
        Blocks.insertAll(blockList:_*)
        Transactions.insertAll(transactionList:_*)
        blockList = Nil
        transactionList = Nil
        outputList = Nil
        //inputList = List.empty
        counter = 0
        val totalTime = System.currentTimeMillis - startTime
        println("Saved until block nr. "+blockCount+" in "+ totalTime +"ms")
      }


      blockCount += 1

      blockList = (blockHash, blockCount)::blockList

      for (trans <- block.getTransactions){
        transactionCount+=1
        val transactionHash = trans.getHashAsString()
        transactionList = (transactionHash, transactionCount, blockCount)::transactionList


        /*if (!trans.isCoinBase)
        {
          for (input <- trans.getInputs)
          {
            val outpoint_transaction_hash = input.getOutpoint.getHash.toString
            val outpoint_index = input.getOutpoint.getIndex
            inputList = (transaction_hash,0:Long,0,"-",outpoint_transaction_hash,outpoint_index)::inputList
            //println(counter);
            counter+=1
          }
        } */
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
          val addressId = addressMap.getOrElseUpdate(addressHash,{addressCount+=1;addressCount} )
          val value = output.getValue.intValue


          outputList = (transactionCount, addressId, index, value)::outputList
          //println(counter);
          counter+=1
          index+=1
        }
      }
    }
  }
}













