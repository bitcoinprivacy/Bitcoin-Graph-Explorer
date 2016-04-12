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

  for(script <- Source.fromFile(scriptsFile).getLines)
    processScript(getSecondElement(script))
  printCommandList

  def isJustHash(string: String): Boolean =
    string.head == '['

  def isJustPushData(instruction: String): Boolean =
    instruction.contains("PUSHDATA1") ||
      instruction.contains("PUSHDATA2") ||
      instruction.contains("PUSHDATA4")

  def isJustNumeric(x: String): Boolean =
    "0123456789".contains(x)

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
    if (isJustPushData(instruction))
      updateCommandList("pushdata")
    else if (isJustHash(instruction))
      updateCommandList("hashes")
    else if (isJustNumeric(instruction))
      updateCommandList("numeric")
    else
      updateCommandList(instruction)

  def updateCommandList(instruction: String): Unit =
    if (commands.contains(instruction))
      commands.update(instruction, commands.getOrElse[Int](instruction, {1}) + 1)
    else
      commands.put(instruction, 1)

  def printCommandList: Unit = {
    for (command <- commands.toSeq.sortBy(_._1))
      println(command._1 + ": " + command._2)
  }
}