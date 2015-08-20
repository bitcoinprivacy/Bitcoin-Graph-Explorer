package actions

import core._

import scala.slick.driver.JdbcDriver.simple._
import scala.slick.jdbc._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._

class PopulateClosure(blockHeights: Vector[Int]) extends AddressClosure(blockHeights)
{
  val table = LmdbMap.create("closures")
  override lazy val unionFindTable = new ClosureMap(table)

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
      val (parentOption, newTree) = tree.find(value)
      for (parent <- parentOption )
        {
          queries +:= (value.array.toArray, parent.array.toArray)
        }

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
        println("DEBUG: Saved until element %s in %s s, %s µs per element" format (counterTotal, (System.currentTimeMillis - timeStart)/1000, (System.currentTimeMillis - timeStart)*1000/(counterTotal+1)))
      }
      newTree
    }

    println("DONE: Saved until element %s in %s s, %s µs per element" format (counterTotal, (System.currentTimeMillis - timeStart)/1000, (System.currentTimeMillis - timeStart)*1000/(counterTotal+1)))

    saveElementsToDatabase(queries, counter)

    table.commit

    totalElements
  }

  def saveElementsToDatabase(queries: Vector[(Array[Byte], Array[Byte])], counter: Int): Unit =
  {
    val start = System.currentTimeMillis
    transactionDBSession {
      try{ addresses.insertAll(queries: _*) } catch {
        case e: java.sql.BatchUpdateException => throw(e.getNextException)

      }
    }
  }

}


