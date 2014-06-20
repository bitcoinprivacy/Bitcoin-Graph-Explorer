package actions

import libs._

import scala.slick.session.Database
import Database.threadLocalSession
import scala.slick.jdbc.{StaticQuery => Q, StaticQuery0, GetResult}

/**
 * Created by yzark on 12/16/13.
 */

class AddressesBalancer(args:List[String])
{
  def readElements(start: Int, step: Int) =
  {
    // weird trick to allow slick using Array Bytes
    implicit val GetByteArr = GetResult(r => r.nextBytes())
    val timeStart = System.currentTimeMillis
    println ("     Reading " + balanceTransactionSize + " elements from the Database")
    val query = "SELECT SUM(value) as s, address as a FROM movements m where spent_in_transaction_hash IS NULL group by a limit %s, %s"
    val elements = Q.queryNA[(Double, Array[Byte])](query format (start, balanceTransactionSize))
    println ("     Read elements in " + (System.currentTimeMillis - timeStart) + " ms ")

    elements
  }

  def convertToQueryList(values:  StaticQuery0[(Double, Array[Byte])]): List[String] =
  {

    val timeStart = System.currentTimeMillis
    println ("     Converting values to query")
    var arrQueries:List[String] = List()

    for (value <- values)
    {
      arrQueries = "update addresses set balance = " + value._1 + " where hash = " + Hash(value._2)::arrQueries
    }

    println("     Converted values in % ms" format (System.currentTimeMillis - timeStart))

    arrQueries
  }

  def saveQueryListToDatabase(arrQueries:List[String]) =
  {
    val timeStart = System.currentTimeMillis
    val length = arrQueries.length
    println ("     Copying " + length + " elements to the Database")
    (Q.u + "BEGIN TRANSACTION").execute
    for (query <- arrQueries)    (Q.u + query).execute
    (Q.u + "COMMIT TRANSACTION").execute
    println ("     Copied elements % elements in %s" + (System.currentTimeMillis - timeStart) + " ms ")

    length
   }

  databaseSession
  {
    implicit val GetByteArr = GetResult(r => r.nextBytes())
    val timeStart = System.currentTimeMillis
    println("Calculating balance...")
    val start = if (args.length>0) args(0).toInt else 0
    val end = if (args.length>1) args(1).toInt else countInputs
    var counter = 0
    println("Searching in inputs from %s to %s" format (start, end))
    for (i <- start to end by balanceTransactionSize)
    {
      println("=============================================")
      counter += saveQueryListToDatabase(convertToQueryList(readElements(i, balanceTransactionSize)))
      println("=============================================")
    }

    println
    println("/////////////////////////////////////////////")
    println("Balance generated in " + (System.currentTimeMillis - timeStart) + " ms ")
    println("/////////////////////////////////////////////")
    println
  }
}
