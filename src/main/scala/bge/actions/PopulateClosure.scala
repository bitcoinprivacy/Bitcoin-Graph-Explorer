package actions

import core._

import scala.slick.driver.JdbcDriver.simple._
import util._
import Hash._

class PopulateClosure(blockHeights: Vector[Int]) extends AddressClosure(blockHeights)
{
  lazy val table = LmdbMap.create("closures")
  override lazy val unionFindTable = new ClosureMap(table)

  def saveTree: Int =
  {
    val tree = generatedTree
    val timeStart = System.currentTimeMillis
    var queries: Vector[(Array[Byte], Array[Byte])] = Vector()
    val totalElements = tree.elements.size
    var counter = 0
    var counterTotal = 0
    var counterFinal = 0

    table.commit

    for (key <- tree.elements.keys)
    {
      val parent = tree.onlyFind(key) // this is a hack to only copy the db, without find improvements
      queries +:= (hashToArray(key), hashToArray(parent))  // because it appears to be bad to iterate and write at the same time in lmdb

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
        log.info("Saved to DB until element %s in %s s, %s µs per element" format (counterTotal, (System.currentTimeMillis - timeStart)/1000, (System.currentTimeMillis - timeStart)*1000/(counterTotal+1)))
      }
      // newTree
    }

    log.info("Saved to DB until element %s in %s s, %s µs per element" format (counterTotal, (System.currentTimeMillis - timeStart)/1000, (System.currentTimeMillis - timeStart)*1000/(counterTotal+1)))

    saveElementsToDatabase(queries, counter)
    syncAddressesWithUTXOs

    table.close

    totalElements
  }

  def saveElementsToDatabase(queries: Vector[(Array[Byte], Array[Byte])], counter: Int): Unit =
  {
    val start = System.currentTimeMillis
    DB withSession { implicit session =>
      try{ addresses.insertAll(queries: _*) } catch {
        case e: java.sql.BatchUpdateException => throw(e.getNextException)
      }
    }
  }
}


