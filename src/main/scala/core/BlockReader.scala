package core

// for blocks db and longestChain
import com.google.bitcoin.core._
import util._

import scala.collection.JavaConverters._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

// extends libs.BlockSource means that it depends on a libs.BlockSource
trait BlockReader extends BlockSource {

  def saveTransaction(transaction: Transaction)
  def saveBlock(b: Hash): Unit
  def pre: Unit
  def post: Unit
  

  var savedBlockSet: Set[Hash] = Set.empty
  val longestChain = getLongestBlockChainHashSet

  transactionDBSession
  {
    pre

    val savedBlocks = for (b <- blockDB)
      yield (b.hash)

    for (c <- savedBlocks)
      savedBlockSet = savedBlockSet + Hash(c)

    var a = 1
    var time = System.currentTimeMillis()

    for (transaction <- transactionSource)
    {
      saveTransaction(transaction)

      if (a % 1000 == 0)
      {
        val t = System.currentTimeMillis()  - time
        System.out.println("Processed " + a + " transactions in " + t + " using " + 1000 * t / a + " µs/tx");
      }

      a += 1
    }

    post
  }

  def blockFilter(b: Block) =
  {
    val blockHash = Hash(b.getHash.getBytes)
    (longestChain contains blockHash) && 
      !(savedBlockSet contains blockHash)
  }

  def withoutDuplicates(b: Block, t: Transaction): Boolean =
    !(Hash(b.getHash.getBytes) == Hash("00000000000a4d0a398161ffc163c503763b1f4360639393e0e4c8e300e0caec")  &&
      Hash(t.getHash.getBytes) == Hash("d5d27987d2a3dfc724e359870c6644b40e497bdc0589a033220fe15429d88599")) &&
    !(Hash(b.getHash.getBytes) == Hash("00000000000a4d0a398161ffc163c503763b1f4360639393e0e4c8e300e0caec")  &&
      Hash(t.getHash.getBytes) == Hash("d5d27987d2a3dfc724e359870c6644b40e497bdc0589a033220fe15429d88599"))

  lazy val filteredBlockSource =
      blockSource withFilter blockFilter
    
    def transactionsInBlock(b: Block) = b.getTransactions.asScala filter (t => withoutDuplicates(b,t))

    def inputsInTransaction(t: Transaction) =
      if (!t.isCoinBase)        t.getInputs.asScala
      else                          List.empty

    def outputsInTransaction(t: Transaction) = { 
      t.getOutputs.asScala

    }

    lazy val transactionSource:Iterator[Transaction] = filteredBlockSource flatMap {b => saveBlock(Hash(b.getHash.getBytes)) ; transactionsInBlock(b)}


  def getAddressFromOutput(output: TransactionOutput): Option[Array[Byte]] =
    try
    {
      val address = output.getScriptPubKey.getToAddress(params)
      Some(Array(address.getVersion.toByte)++address.getHash160)
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
            import com.google.bitcoin.core.Utils._
            val pubkey = Hash(pubkeystring).array.toArray
            val address = new Address(params, sha256hash160(pubkey))
            Some(Array(address.getVersion.toByte)++address.getHash160)
          } else { // special case because bitcoinJ doesn't support pay-to-IP scripts
            None
          }
        }
        catch {
          case e: ScriptException =>
            println("bad transaction output: " + output.getParentTransaction.getHash)
            None
        }
      
    }
  

}
