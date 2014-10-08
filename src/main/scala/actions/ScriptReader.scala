package actions

import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.io.Source


// Test object to process script - outputs
class ScriptReader
{
  val params: NetworkParameters = MainNetParams.get
  val commands: HashMap[String, Int] = new mutable.HashMap[String, Int]()
  val excludes: List[Char] = List('[','0','1','2','3','4','5','6','7','8','9')
  val scriptsFile: String = "blockchain/scripts.log"

  for(script <- Source.fromFile(scriptsFile).getLines())
    processScript(script)

  for (command <- commands)
    println(command._1 + ": " + command._2)

  def isExcluded(char: Char): Boolean =
  {
    for (c <- excludes) {
      if (char == c) {
        return true

      }
    }

    return false
  }

  def processScript(script: String): Unit = {
    if (script.isEmpty)
      updateInstructionUsage("Empty")
    else
      getAddressFromScript(script)
  }

  def getAddressFromScript(instruction: String): Unit = {
    if (instruction.contains(' '))
      processInstructionsList(instruction.split(" ").toList)
    else
      processInstruction(instruction)
  }

  def processInstructionsList(instructions: List[String]) =
    for (instruction: String <- instructions)
      processInstruction(instruction)

  def processInstruction(instruction: String): Unit = {
    if (isExcluded(instruction.head))
      updateInstructionUsage("hashes")
    else
      updateInstructionUsage(instruction)
  }

  def getVersionedHashFromAddress(address: Address) = {
    (Array(address.getVersion.toByte) ++ address.getHash160).toArray
  }

  def updateInstructionUsage(instruction: String): Unit = {
    if (commands.contains(instruction))
      commands.update(instruction, commands.getOrElse[Int](instruction, {1}) + 1)
    else
      commands.put(instruction, 1)
  }
} 
