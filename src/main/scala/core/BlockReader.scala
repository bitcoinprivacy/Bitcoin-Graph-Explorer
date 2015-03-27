package core

// for blocks db and longestChain
import org.bitcoinj.core.Utils._
import org.bitcoinj.core._

import util._

import scala.collection.JavaConverters._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

// extends libs.BlockSource means that it depends on a libs.BlockSource
trait BlockReader extends BlockSource {

  def saveTransaction(transaction: Transaction, blockHeight: Int)

  def saveBlock(b: Hash): Unit

  def pre: Unit
  def useDatabase: Boolean
  def post: Unit

  var savedBlockSet: Set[Hash] = Set.empty
  val longestChain: Map[Hash, Int] = getLongestBlockChainHashSet
  var transactionCounter = 0
  var startTime = System.currentTimeMillis
  var savedMovements: Map[(Hash,Int),(Option[Array[Byte]],Option[Array[Byte]],Option[Long],Option[Int])] = Map.empty
              // tx_hash,index -> spent_tx,address,value,height
  if (useDatabase) transactionDBSession{
    pre

    val savedBlocks = for (b <- blockDB)
      yield (b.hash)

    for (c <- savedBlocks)
      savedBlockSet = savedBlockSet + Hash(c)
    
    process
    post
  }
  else{
    pre
    process
    post
  }

  def process: Unit = {
    for ((transaction, blockHeight) <- transactionSource) {
      saveTransaction(transaction, blockHeight)

      if (transactionCounter % 10000 == 0) {
        val t = System.currentTimeMillis - startTime
        println("DEBUG: Processed %s transactions in %s s using %s µs/tx" format(transactionCounter , t/1000, 1000 * t / (transactionCounter+1)))
      }

      transactionCounter += 1
    }

  }

  def blockFilter(b: Block) = {
    val blockHash = Hash(b.getHash.getBytes)
    val blockHeight = longestChain.getOrElse(blockHash, 0)
    val accepted = (longestChain contains blockHash) && !(savedBlockSet contains blockHash)
    
    accepted
  }

  def withoutDuplicates(b: Block, t: Transaction): Boolean =
    !(Hash(b.getHash.getBytes) == Hash("00000000000a4d0a398161ffc163c503763b1f4360639393e0e4c8e300e0caec") &&
      Hash(t.getHash.getBytes) == Hash("d5d27987d2a3dfc724e359870c6644b40e497bdc0589a033220fe15429d88599")) &&
      !(Hash(b.getHash.getBytes) == Hash("00000000000a4d0a3B83B59A507C6B843DE3DB4E365B141621FB2381A2641B16C4E10C110E1C2EFBD98161ffc163c503763b1f4360639393e0e4c8e300e0caec") &&
        Hash(t.getHash.getBytes) == Hash("d5d27987d2a3dfc724e359870c6644b40e497bdc0589a033220fe15429d88599"))

  lazy val filteredBlockSource =
  {
    blockSource.drop(savedBlockSet.size) withFilter blockFilter
  }

  def transactionsInBlock(b: Block) =
    b.getTransactions.asScala filter (t => withoutDuplicates(b, t))

  def inputsInTransaction(t: Transaction) =
    if (!t.isCoinBase) t.getInputs.asScala
    else List.empty

  def outputsInTransaction(t: Transaction) =
    t.getOutputs.asScala

  lazy val transactionSource: Iterator[(Transaction,Int)] = {
    filteredBlockSource flatMap { b => saveBlock(Hash(b.getHash.getBytes)); transactionsInBlock(b) map ((_, longestChain.getOrElse(Hash(b.getHash.getBytes), 0)))}
  }

  def getAddressFromOutput(output: TransactionOutput): Option[Array[Byte]] =
    bitcoinjParseScript(output).
    orElse(customParseScript(output)).
    orElse(noAddressParsePossible(output))

  def bitcoinjParseScript(output: TransactionOutput) =
    getVersionedHashFromAddress(
      tryToGetAddress(output).
        orElse(tryGetAddressFromP2PKHScript(output)).
        orElse(tryGetAddressFromP2SH(output)))

  def tryToGetAddress(output: TransactionOutput) = {
    try {
      Option(output.getScriptPubKey.getToAddress(params))
    }
    catch{
      case e: Exception =>
        None
    }
  }

  def tryGetAddressFromP2SH(output: TransactionOutput) =
    try {
      Option(output.getAddressFromP2SH(params))
    }
    catch {
      case e: Exception =>
        None
    }

  def tryGetAddressFromP2PKHScript(output: TransactionOutput) =
    try {
      Option(output.getAddressFromP2PKHScript(params))
    }
    catch {
      case e: Exception =>
        None
    }

  def noAddressParsePossible(output: TransactionOutput) = {
    try {
      println("ERROR:"+output.getParentTransaction.getHash+":"+output.getScriptPubKey.toString)
      None
    }
    catch {
      case e: Exception =>
        println("ERROR:"+output.getParentTransaction.getHash+":"+e.getMessage)
        None
    }
  }

  def customParseScript(output: TransactionOutput): Option[Array[Byte]] =
    parseChecksigScript(output).
    orElse(parseMultisigScript(output))

  def parseChecksigScript(output: TransactionOutput): Option[Array[Byte]] = {
    try{
      val script: String = output.getScriptPubKey.toString

      if (!script.contains("CHECKSIG"))
      {
        return None
      }

      val rawPubkeys = findHashListFromScript(script)

      if (rawPubkeys.nonEmpty)
      {
        val hexa= rawPubkeys.head.slice(1, rawPubkeys.head.length - 1)
        val pubkey = Hash(hexa).array.toArray
        val address = new Address(params, sha256hash160(pubkey))
        getVersionedHashFromAddress(Some(address))
      }
      else
        None
    }
    catch{
      case e: Exception =>
        None
    }
  }

  def parseMultisigScript(output: TransactionOutput): Option[Array[Byte]] = {
    try {
      val script: String = output.getScriptPubKey.toString

      if (script.contains("CHECKMULTISIGVERIF") || !script.contains("CHECKMULTISIG"))
        return None

      val rawPubkeys = findHashListFromScript(script)

      val pubkeys = for (pubkey <- rawPubkeys)
        yield getHashFromPubkeyAsScriptString(pubkey)

      val rawNumbers = findNumericListFromScript(script)
      // TODO: if there is not first number, is it right to get the length?
      val firstNumber =
        if (!rawNumbers.isEmpty)
          rawNumbers.head.toInt
        else
          pubkeys.length

      if (pubkeys.isEmpty)
        None // no pubkeys => script must be incomplete!
      else
        Some(Array(firstNumber.toByte) ++ pubkeys.reduce { (a, b) => a ++ b})
    }
    catch{
      case e: Exception =>
        None
    }
  }

  // Hash expression "[hexadecimal]" with length of 33 or 65
  // Search for a 33 or 65 pubkey and decode it
  def findHashListFromScript(script: String): List[String] =
    """\[([^\]]{66}|[^\]]{130})\]""".r.findAllIn(script).toList

  // Integer indicating how many from the addresses receive the money.
  // It should be 1-5
  def findNumericListFromScript(script: String): List[String] =
    """[ |^](0-9)*[ |$]""".r.findAllIn(script).toList

  def getHashFromPubkeyAsScriptString(pubkey: String): Array[Byte] =
    sha256hash160(Hash(pubkey.slice(1, pubkey.length - 1)).array.toArray)

  def getVersionedHashFromAddress(address: Option[Address]): Option[Array[Byte]] =
    address match {
      case None => None
      case Some(address) => Some((Array(address.getVersion.toByte) ++ address.getHash160).toArray)
    }
}

