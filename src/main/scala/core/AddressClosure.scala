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
  def adaptTreeIfNecessary(tree: DisjointSets[Hash]):  DisjointSets[Hash] = tree
  
  def generateTree: DisjointSets[Hash]

  def insertInputsIntoTree(addresses: Iterable[Hash], tree: DisjointSets[Hash]): DisjointSets[Hash] =
  {
    val addedTree = addresses.foldLeft(tree)((t:DisjointSets[Hash],a:Hash) => t.add(a))
    addedTree.union(addresses)
  }
    

  def insertValuesIntoTree(databaseResults: HashMap[Hash, Array[Hash]], tree: DisjointSets[Hash]) =
  {
    println("Insering values into tree");
    val start = System.currentTimeMillis

    databaseResults.foldLeft(tree)((t,l) => insertInputsIntoTree(l._2,t)) 
    
    println("Values inserted")
    
  }

  def saveTree(tree: DisjointSets[Hash]): Int =
  {
    val timeStart = System.currentTimeMillis
    var queries: Vector[(Array[Byte], Array[Byte])] = Vector()
    val totalElements = tree.elements.size
    var counter = 0
    var counterTotal = 0
    
    println("DEBUG: Saving tree to database...")
    var counterFinal = 0
    tree.elements.keys.foldLeft(tree){(t,value) => 
      val (parent, newTree) = tree.find(value)
      queries +:= (value.array.toArray, parent.array.toArray)
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
      newTree
    }
    
    println("DONE: Saved until element %s in %s s, %s µs per element" format (counterTotal, (System.currentTimeMillis - timeStart)/1000, (System.currentTimeMillis - timeStart)*1000/counterTotal+1))

    saveElementsToDatabase(queries, counter)
    
    totalElements
  }

  def saveElementsToDatabase(queries: Vector[(Array[Byte], Array[Byte])], counter: Int): Unit =
  {
    val start = System.currentTimeMillis
    transactionDBSession {
      addresses.insertAll(queries: _*)
    }
  }
  println("applying closure ")
  val timeStart = System.currentTimeMillis

  val countSave = saveTree(adaptTreeIfNecessary(generateTree))

  val totalTime = System.currentTimeMillis - timeStart

  println("DONE: Total of %s addresses closured in %s s, %s µs per address" format
    (countSave, totalTime / 1000, 1000 * totalTime / (countSave + 1)))
}
 
 
