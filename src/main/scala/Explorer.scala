import java.io.File

import actions._
import util._
import core._

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/1
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
object Explorer extends App{
  args.toList match{
    // we could write a version of populate that just
    // parse the blocks - tx - outputs - inputs
    case "test-addresses"::rest =>
      object TestBlockReader extends BitcoinDRawFileBlockSource with TestBlockReader //needs to be in this order for linearization
      TestBlockReader
    case "analyze-script"::rest             =>
      (new ScriptReader)
    case "test-script"::rest             =>
      (new ScriptTester)
    case "reader"::rest           =>
      object InitializeBlockReader extends BitcoinDRawFileBlockSource with FastBlockReader //needs to be in this order for linearization
      new File(transactionsDatabaseFile).delete
      InitializeBlockReader
    case "closure"::rest              =>
      FastAddressClosure
    case "balance"::rest         =>
      FastAddressBalance
    case "populate"::rest             =>
      object InitializeBlockReader extends BitcoinDRawFileBlockSource with FastBlockReader //needs to be in this order for linearization
      new File(transactionsDatabaseFile).delete
      InitializeBlockReader
      FastAddressClosure
      FastAddressBalance
    case "resume"::rest               => 
      object ResumeBlockReader extends BitcoinDRawFileBlockSource with SlowBlockReader //needs to be in this order for linearization
      ResumeBlockReader
      new SlowAddressClosure(ResumeBlockReader.savedMovements)
      new SlowAddressBalance(ResumeBlockReader.savedMovements)
      SlowStatistics
      SlowClosureGini
      SlowAddressGini
      SlowRichestAddresses
      SlowRichestClosures
    case "gini"::rest =>
      SlowAddressGini
      SlowClosureGini
    case _=> println("""
      Available commands:
       reader:
       closure:
       balance:
       populate: create the database movements with movements and closures.
       resume: update the database generated by populate with new incomming data.
       test-addresses: read every block and try to generate address for every single output.
       test-script: read the blockchain/scripts.log file and perform a custom test over each line.
       analyze-script: read and print an inform from blockchain/scripts.log.
    """)
  }
}
