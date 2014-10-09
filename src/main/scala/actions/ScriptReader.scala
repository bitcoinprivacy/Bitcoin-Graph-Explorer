package actions

import java.util.NoSuchElementException

import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.io.Source


// Test object to process script - outputs
class ScriptReader {
  val params: NetworkParameters = MainNetParams.get
  val commands: HashMap[String, Int] = new mutable.HashMap[String, Int]()
  val excludes: List[Char] = List('[','0','1','2','3','4','5','6','7','8','9')
  val scriptsFile: String = "blockchain/scripts.log"

  val test = """\[(.)+\]""".r.findAllIn("asdasd [a1234]")
  println(test.toList)

  /*for(script <- Source.fromFile(scriptsFile).getLines())
    processScript(getSecondElement(script))

  for (command <- commands.toSeq.sortBy(_._1))
    println(command._1 + ": " + command._2)
  */
  def isHexadecimal(char: Char): Boolean =
    excludes.contains(char)

  def getSecondElement(string: String): String =
    if (string.contains(':'))
      string.substring(string.indexOf(':')+1)
    else
      string

  def processScript(script: String): Unit =
    if (script.isEmpty||script.equals(""))
      updateInstructionUsage("Empty")
    else
      getAddressFromScript(script)

  def getAddressFromScript(instruction: String): Unit =
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
      updateInstructionUsage("hashes")
    else
      updateInstructionUsage(instruction)

  def getVersionedHashFromAddress(address: Address) =
    (Array(address.getVersion.toByte) ++ address.getHash160).toArray

  def instructionInvalid(instruction: String): Boolean =
    instruction.contains("PUSHDATA")

  def updateInstructionUsage(instruction: String): Unit =
    if (commands.contains(instruction))
      commands.update(instruction, commands.getOrElse[Int](instruction, {1}) + 1)
    else
      commands.put(instruction, 1)
}