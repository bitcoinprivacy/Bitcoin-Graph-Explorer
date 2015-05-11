package actions

import core._
import util._
import java.io._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

object FastAddressClosure extends AddressClosure
{
  def getAddressesFromMovements(firstElement: Int, elements: Int): HashMap[Hash, Array[Hash]] =
  {
    val timeStart = System.currentTimeMillis
    
    val mapAddresses:HashMap[Hash, Array[Hash]] = HashMap.empty
    val emptyArray = Hash.zero(0).array.toArray
    println("Reading")
    
    val queried =
      for (q <- movements.drop(firstElement).take(elements).filter{ q =>
        val stx = q.spent_in_transaction_hash
        lazy val ad =  q.address
        stx.isDefined && stx =!= emptyArray && ad.isDefined && ad =!= emptyArray
      })
      yield (q.spent_in_transaction_hash, q.address)

    for {
      q <- queried
      sTx <- q._1
      ad <- q._2}
    {
      // address cannot be empty if slick works propertly
      // spent_in_transaction is no empty too
      val spentInTx = Hash(sTx)
      val addr = Hash(ad)
      val list: Array[Hash] = mapAddresses.getOrElse(spentInTx, Array())
      mapAddresses.update(spentInTx, list :+ addr)
    }

    println("Read")
    
    mapAddresses
  }

  def generateTree: (HashMap[Hash, DisjointSetOfAddresses])  =
  {
    val timeStart = System.currentTimeMillis
    println("DEBUG: Generating tree ...")
    val tree:HashMap[Hash, DisjointSetOfAddresses] = HashMap.empty
    transactionDBSession {
      println("counting inputs");
      val end = 370000000  // a number is bigger than the max of inputs, countInputs is way to slow,
                           // even tried direct in mysql, select count(*) need several minutes
      println("counted " + end + " inputs in " + (System.currentTimeMillis - timeStart)/1000);
      
      for (i <- 0 to end by closureReadSize)
      {
        println("DEBUG: Loading elements up to "+i)
        val amount = if (i + closureReadSize > end) end - i else closureReadSize
        insertValuesIntoTree(getAddressesFromMovements(i, amount), tree)
        println("DEBUG: Loaded until element %s in %s s, %s µs per element"
          format
          (i+amount,
            (System.currentTimeMillis - timeStart)/1000,
            (System.currentTimeMillis - timeStart)*1000/(amount+i+1)))
      }
    }

    println("DONE: Tree generated in %s ms" format (System.currentTimeMillis - timeStart))
    
    tree
  }
}
