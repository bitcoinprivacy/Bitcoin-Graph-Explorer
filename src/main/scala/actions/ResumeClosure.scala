package actions

import core._
import java.util.Calendar
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._
import util.Hash._

class ResumeClosure(blockHeights: Vector[Int]) extends AddressClosure(blockHeights: Vector[Int]) {

  lazy val table = LmdbMap.open("closures")
  override lazy val unionFindTable = new ClosureMap(table)

  override def insertInputsIntoTree(addressList: Iterable[Hash], tree: DisjointSets[Hash]): DisjointSets[Hash] =
  {
    val (pairList, tree1) = addressList.foldLeft((List[(Hash,Option[Hash])](),tree)){
      case ((l,t),a) =>
        val (repOpt,treex) = t.find(a)
        (l:+(a,repOpt),treex)
    }

    val addedTree = addressList.foldLeft(tree1)((t:DisjointSets[Hash],a:Hash) => t.add(a))
    val tree2 = addedTree.union(addressList)

    // update the postgres DB

    // must be at least 2 addresses
    assert(addressList.size >= 2, "union of trivial closure")
    val (representantOpt,result) = tree2 find (addressList.head)
    val newRep = representantOpt.get

    transactionDBSession {

      currentStat.total_closures+=pairList.filter( pair => pair match {
        case (a, Some(oldRep)) if (oldRep != newRep) =>  true
        case _ =>                          false
      }).length

      for ((address, oldRepOpt) <- pairList)
        oldRepOpt match
        {
          case None => insertAddress(address,newRep)   //insert into DB
          case Some(oldRep) if (oldRep != newRep) => // if representant new, update everything that had the old one
            val updateQuery = for(p <- addresses if p.representant === hashToArray(oldRep)) yield p.representant
            updateQuery.update(newRep) // TODO: compile query, refactor to BitcoinDB

          case _ => // nothing to do
        }
    }

    result
  }

 
  override def saveTree(tree: DisjointSets[Hash]): Int =  // don't replace the postgres DB
  {
    val no = tree.elements.size - startTableSize // return the number of new elements in the union-find structure
    table.close // but don't forget to flush
    saveAddresses
    no
  }

  lazy val vectorAddresses: collection.mutable.Map[Hash, Hash] = collection.mutable.Map()

  def insertAddress(s: (Hash, Hash)) =
  {
    vectorAddresses += s

    if (vectorAddresses.size >= populateTransactionSize)
      saveAddresses
  }

  def saveAddresses = {
    println("DEBUG: Inserting Addresses into SQL database ...")

    val convertedVector =vectorAddresses map (p => (hashToArray(p._1), hashToArray(p._2)))

    try{
      transactionDBSession(addresses.insertAll(convertedVector.toSeq:_*))
    }
    catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }

    currentStat.total_addresses+=vectorAddresses.size
    vectorAddresses.clear

    println("DEBUG: Data inserted")

  }
}


