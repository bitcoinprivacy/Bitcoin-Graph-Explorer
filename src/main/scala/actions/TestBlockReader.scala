package actions

import java.io.File

import core._
import util._

import scala.collection.immutable

// for blocks db and longestChain
import org.bitcoinj.core._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

// Example of how to create a test-class
// just try to generate address from outputs, no database
trait TestBlockReader extends BlockReader
{
  def useDatabase: Boolean = false

  def saveTransaction(trans: Transaction, blockHeight: Int) =
  {
    val transactionHash = Hash(trans.getHash.getBytes)

    for (input <- inputsInTransaction(trans))
      testInput(input)

    for (output <- outputsInTransaction(trans))
      getAddressFromOutput(output: TransactionOutput).
      orElse(testOutput(output: TransactionOutput))
  }

  def pre  = {
    // Add here code to test
  }

  def post = {
    // Add here code to test
  }

  def saveBlock(b: Hash, txs: Int, btcs: Long, tstamp: Long): Unit = {
    // Add here code to test
  }

  def testInput(input: TransactionInput): Unit = {
    // Add here code to test
  }

  def testOutput(output: TransactionOutput): Option[Array[Byte]] = {
    // Add here code to test
    None
  }
}
