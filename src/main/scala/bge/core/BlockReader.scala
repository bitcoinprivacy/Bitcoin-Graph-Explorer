package core

import org.bitcoinj.core.Utils._
import org.bitcoinj.core._

import util._

import scala.collection.JavaConverters._
import scala.slick.driver.JdbcDriver.simple._

// extends BlockSource means that it depends on a BlockSource
trait BlockReader extends BlockSource {

  def saveTransaction(transaction: Transaction, blockHeight: Int): Unit

  def finishBlock(b: Hash, txs: Int, btcs: Long, tstamp: Long, height: Int): Unit

  def pre: Unit
  def useDatabase: Boolean
  def post: Unit

  var processedBlocks: Vector[Int] = Vector.empty
  var savedBlockSet: Set[Hash] = Set.empty
//  val longestChain: Map[Hash, Int] = getLongestBlockChainHashSet
  var transactionCounter = 0
  var startTime = System.currentTimeMillis

  if (useDatabase) DB withSession { implicit session =>
    pre

    val savedBlocks = for (b <- blockDB)
      yield b.hash

    for (c <- savedBlocks)
      savedBlockSet += Hash(c)

    process
    post
  }
  else{
    pre
    process
    post
  }

  def process: Unit = {
      for ((block, height) <- filteredBlockSource) {
        for (transaction <- transactionsInBlock(block)) {
          saveTransaction(transaction, height)
          transactionCounter +=1
        }
        val blockHash = Hash(block.getHash.getBytes)
        finishBlock(blockHash, block.getTransactions.size,getTxValue(block),block.getTimeSeconds,height)
      }
  }

  def blockFilter(b: Block) = {
    val blockHash = Hash(b.getHash.getBytes)
    val exists = (savedBlockSet contains blockHash)
    !exists 
  }

  lazy val filteredBlockSource =
    blockSource withFilter (p=>blockFilter(p._1))


  def transactionsInBlock(b: Block) =
    b.getTransactions.asScala // filter (t => withoutDuplicates(b, t)) ( see above )

  def inputsInTransaction(t: Transaction) =
    if (!t.isCoinBase) t.getInputs.asScala
    else List.empty

  def outputsInTransaction(t: Transaction) =
    t.getOutputs.asScala

  def getTxValue(b: Block) =
    (for {
       t: Transaction <- transactionsInBlock(b)
       o: TransactionOutput <- outputsInTransaction(t)
     }
    yield o.getValue.value).sum

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
      //println("ERROR:"+output.getParentTransaction.getHash+":"+output.getScriptPubKey.toString)
      // TODO: save it to the database?
      None
    }
    catch {
      case e: Exception =>
        //println("ERROR:"+output.getParentTransaction.getHash+":"+e.getMessage)
        // TODO: save it to the database?
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
