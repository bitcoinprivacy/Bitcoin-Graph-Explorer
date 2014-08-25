import java.io.File

import actions._
import util._
import core._

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/13
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
object Explorer extends App{
  args.toList match{
    case "populate"::rest             => object InitializeBlockReader extends BitcoinDRawFileBlockSource with FastBlockReader //needs to be in this order for linearization
      new File(transactionsDatabaseFile).delete
      InitializeBlockReader
    case "resume"::rest               => object ResumeBlockReader extends BitcoinDRawFileBlockSource with SlowBlockReader //needs to be in this order for linearization
      ResumeBlockReader
    case "closure"::rest              => new AddressesClosurer(rest)
    case _=> println("""
      Available commands:
      populate [number of blocks]
      closure
    """)
  }
}
