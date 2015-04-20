package core

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/13
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
import util._
import java.io._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import java.lang.System



trait AddressClosure
{
  def adaptTreeIfNecessary(tree:  HashMap[Hash, DisjointSetOfAddresses]):  HashMap[Hash, DisjointSetOfAddresses] = tree
  def createIndexesIfNecessary = { }

  def generateTree: HashMap[Hash, DisjointSetOfAddresses]

  def insertValuesIntoTree(databaseResults: HashMap[Hash, Array[Hash]], tree: HashMap[Hash, DisjointSetOfAddresses]) =
  {
    val start = System.currentTimeMillis

    for (t <- databaseResults)
    {
      val dSOAs= t._2 map(a => tree.getOrElseUpdate(a, {DisjointSetOfAddresses(a)}) )

      def union(l:Array[DisjointSetOfAddresses]): Unit = l match{
        case Array() =>
        case Array(x) =>
        case ar => ar(0).union(ar(1)) ; union(ar.drop(1))
      }

      union(dSOAs)
    }

  }

  def saveTree(tree: HashMap[Hash, DisjointSetOfAddresses]): Int =
  {
    val timeStart = System.currentTimeMillis
    var queries: Vector[(Array[Byte], Array[Byte], Option[Long])] = Vector()
    val totalElements = tree.size
    var counter = 0
    var counterTotal = 0
    
    println("DEBUG: Saving tree to database...")
    var counterFinal = 0
    for (value <- tree)
    {
      queries +:= (value._1.array.toArray, value._2.find.address.array.toArray, None)
      counter += 1
      counterTotal += 1
      counterFinal += 1
      if (counter == closureTransactionSize)
      {
        saveElementsToDatabase(queries, counter)
        queries = Vector()
        counter = 0
      }
      if (counterFinal % 1000000 == 0) {
        counterFinal = 0
        println("DEBUG: Saved until element %s in %s s, %s µs per element" format (counterTotal, (System.currentTimeMillis - timeStart)/1000, (System.currentTimeMillis - timeStart)*1000/counterTotal))
      }
    }
    
    println("DONE: Saved until element %s in %s s, %s µs per element" format (counterTotal, (System.currentTimeMillis - timeStart)/1000, (System.currentTimeMillis - timeStart)*1000/counterTotal))

    saveElementsToDatabase(queries, counter)
    
    totalElements
  }

  def saveElementsToDatabase(queries: Vector[(Array[Byte], Array[Byte], Option[Long])], counter: Int): Unit =
  {
    val start = System.currentTimeMillis
    transactionDBSession {
      addresses.insertAll(queries: _*)
    }
  }

  val timeStart = System.currentTimeMillis

  val countSave = saveTree(adaptTreeIfNecessary(generateTree))

  val totalTime = System.currentTimeMillis - timeStart

  createIndexesIfNecessary

  println("DONE: Total of %s addresses closured in %s s, %s µs per address" format
    (countSave, totalTime / 1000, 1000 * totalTime / (countSave + 1)))
}
 
 
