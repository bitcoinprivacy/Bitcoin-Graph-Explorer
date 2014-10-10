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
    case "analize"::rest             =>
      (new ScriptReader).analyze
    case "script"::rest             =>
      (new ScriptReader).testScripts
    case "populate"::rest             =>
      object InitializeBlockReader extends BitcoinDRawFileBlockSource with FastBlockReader //needs to be in this order for linearization
      new File(transactionsDatabaseFile).delete
      InitializeBlockReader
      new FastAddressClosure(List("0", countInputs.toString))
      AddressBalance
    case "resume"::rest               => 
      val a = countInputs
      object ResumeBlockReader extends BitcoinDRawFileBlockSource with SlowBlockReader //needs to be in this order for linearization
      ResumeBlockReader
      new SlowAddressClosure(List(a.toString, countInputs.toString))
      AddressBalance
    case _=> println("""
      Available commands:
      populate
      resume
    """)
  }
}
