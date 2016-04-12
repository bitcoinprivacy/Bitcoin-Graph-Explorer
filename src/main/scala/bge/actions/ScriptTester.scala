package actions

import java.util.NoSuchElementException

import org.bitcoinj.core.Utils._
import org.bitcoinj.core.{Address, NetworkParameters, TransactionOutput}
import org.bitcoinj.params.MainNetParams
import util.Hash

import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.io.Source


// Test object to process script - outputs. It is used to manage the scripts
// that can not be decoded by BlockReader.
// The main purpose is to parse a file with lines containing:
//    tx Hash:script


class ScriptTester {
  val commands: HashMap[String, Int] = new mutable.HashMap[String, Int]()
  val scriptsFile: String = "blockchain/scripts.log"

  for(script <- Source.fromFile(scriptsFile).getLines)
      if (test(script)) updateCommandList("right")
      else              updateCommandList("wrong")

  printCommandList

  // Define here the function to test over the
  // scripts in blockchain/scripts.log
  def test(script: String): Boolean =
    true

  def printCommandList: Unit = {
    for (command <- commands.toSeq.sortBy(_._1))
      println(command._1 + ": " + command._2)
  }

  def getSecondElement(string: String): String =
    if (string.contains(':'))
      string.substring(string.indexOf(':')+1)
    else
      string

 def updateCommandList(instruction: String): Unit =
    if (commands.contains(instruction))
      commands.update(instruction, commands.getOrElse[Int](instruction, {1}) + 1)
    else
      commands.put(instruction, 1)
}