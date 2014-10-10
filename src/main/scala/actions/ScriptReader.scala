package actions

import org.bitcoinj.core.Utils._
import java.util.NoSuchElementException
import org.bitcoinj.core.{TransactionOutput, Address, NetworkParameters}
import org.bitcoinj.core.Utils._
import org.bitcoinj.params.MainNetParams
import util.Hash
import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.io.Source


// Test object to process script - outputs. It is used to manage the scripts
// that can not be decoded by BlockReader.
// The main purpose is to parse a file with lines containing:
//    tx Hash:script


class ScriptReader {
  val params: NetworkParameters = MainNetParams.get
  val commands: HashMap[String, Int] = new mutable.HashMap[String, Int]()
  val excludes: List[Char] = List('[','0','1','2','3','4','5','6','7','8','9')
  val numbers = List('[','0','1','2','3','4','5','6','7','8','9')
  val scriptsFile: String = "blockchain/scripts.log"

  // Get the repetition of each SCRIPT command
  def analyze: Unit = {
    for(script <- Source.fromFile(scriptsFile).getLines)
      processScript(getSecondElement(script))
    printCommandList
  }

  // try to generate address-hash from the script
  def testScripts: Unit = {
    var right = 0
    var wrong = 0

    for(script <- Source.fromFile(scriptsFile).getLines)
      customParseScript(getSecondElement(script)) match {
        case Some(address) => outputAddress(address, script)
        case None => updateCommandList("wrong")
      }

    printCommandList
  }

  def printCommandList: Unit = {
    for (command <- commands.toSeq.sortBy(_._1))
      println(command._1 + ": " + command._2)
  }

  def outputAddress(address: Array[Byte], script: String): Unit = {
    updateCommandList("right")
    updateCommandList("Length " + address.length)
  }

  def isHexadecimal(char: Char): Boolean =
    excludes.contains(char)

  def getSecondElement(string: String): String =
    if (string.contains(':'))
      string.substring(string.indexOf(':')+1)
    else
      string

  def processScript(script: String): Unit =
    if (script.isEmpty||script.equals(""))
      updateCommandList("Empty")
    else
      processScriptString(script)

  def processScriptString(instruction: String): Unit =
    if (!instruction.contains(' ')||instruction.split(' ').toList.isEmpty)
      processInstruction(instruction)
    else
      processInstructionsList(instruction.split(' ').toList)

  def processInstructionsList(instructions: List[String]): Unit =
    try{
      instructions.foreach{processInstruction}}
    catch{
      case e: NoSuchElementException => println("PARSER"+instructions)}

  def processInstruction(instruction: String): Unit =
    if (instructionInvalid(instruction))
      return
    else if (isHexadecimal(instruction.head))
      updateCommandList("hashes")
    else
      updateCommandList(instruction)

  def instructionInvalid(instruction: String): Boolean =
    instruction.contains("PUSHDATA")

  def updateCommandList(instruction: String): Unit =
    if (commands.contains(instruction))
      commands.update(instruction, commands.getOrElse[Int](instruction, {1}) + 1)
    else
      commands.put(instruction, 1)

  def parseChecksigScript(script: String): Option[Array[Byte]] = {
    if (script.contains("PUSHDATA1")|| script.contains("PUSHDATA2") || script.contains("PUSHDATA4"))
      return None

    val start: Int = script.indexOf('[')+1
    var end: Int = script.indexOf(']') - start+1
    updateCommandList("checksig")

    if (end > start + 30)
    {
      val hexa = script.substring(start, end)
      val pubkey = Hash(hexa).array.toArray
      val address = new Address(params, sha256hash160(pubkey))
      getVersionedHashFromAddress(Some(address))
    }
    else
      None
  }

  def customParseScript(script: String): Option[Array[Byte]] = {
    if (!script.contains("CHECKMULTISIGVERIF") && script.contains("CHECKMULTISIG"))
      parseMultisigScript(script)
    else if (script.endsWith("CHECKSIG"))
      parseChecksigScript(script)
    else
      None
  }

  def isAllDigits(x: String) = x forall Character.isDigit

  def parseMultisigScript(script: String): Option[Array[Byte]] = {
    //updateCommandList("multichecksig")
    val rawPubkeys = """\[([^\]])+\]""".r.findAllIn(script).toList
    //updateCommandList("Found " + rawPubkeys.toList.length + " hashes")
    val pubkeys = for (pubkey <- rawPubkeys)
      yield getHashFromPubkeyAsScriptString(pubkey)
    val rawNumbers = """[ |^](0-9)*[ |$]""".r.findAllIn(script).toList
    val firstNumber =
      if (!rawNumbers.isEmpty)
        rawNumbers.head.toInt
      else
        pubkeys.length

    //updateCommandList(firstNumber + " = " + Hash(Array(firstNumber.toByte)))

    if (pubkeys.isEmpty)
      Some(Array(firstNumber.toByte))
    else
      Some(Array(firstNumber.toByte)++pubkeys.reduce{(a,b)=>a++b})
  }

  def getHashFromPubkeyAsScriptString(pubkey: String): Array[Byte] =
    sha256hash160(Hash(pubkey.slice(1, pubkey.length - 1)).array.toArray)

  def getVersionedHashFromAddress(address: Option[Address]): Option[Array[Byte]] =
    address match {
      case None => None
      case Some(address) => Some((Array(address.getVersion.toByte) ++ address.getHash160).toArray)
    }

  def noAddressParsePossible(key: String, output: TransactionOutput) = {
    try {
      println(key+":"+output.getParentTransaction.getHash+":"+output.getScriptPubKey.toString)
    }
    catch{
      case e: Exception =>
      {
        println(key+":"+output.getParentTransaction.getHash+":"+e.getMessage)
      }
    }
    None
  }
}