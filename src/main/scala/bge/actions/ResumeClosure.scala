package actions

import core._
import db._
import scala.slick.driver.PostgresDriver.simple._
import util._
import util.Hash._
import collection.mutable.Map

class ResumeClosure(blockHeights: Vector[Int]) extends AddressClosure(blockHeights: Vector[Int]) with BitcoinDB{

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

    var addRepFlag = 1

    def recordChangedRep(newRep: Hash, oldRep: Hash) = {
      // we store in a DisjoinSets the partial changes
      changedReps = changedReps.add(newRep).add(oldRep).union(Iterable(newRep, oldRep))
    }


    DB withSession { implicit session =>

      for ((address, oldRepOpt) <- pairList.distinct)
        oldRepOpt match
        {
          case None =>
            insertAddress(address,newRep)
            recordChangedRep(newRep,address)
          case Some(oldRep) if (oldRep != newRep) =>
            // if representant new, update everything that had the old one
            val updateQuery = for(p <- addresses if p.representant === hashToArray(oldRep)) yield p.representant
            updateQuery.update(newRep) // TODO: compile query
            recordChangedRep(newRep, oldRep)
 
          case _ => addRepFlag = 0 // one of the elements had this rep before => don't count a new one
        }
    }

    addedReps += addRepFlag

    result

  }


  override def saveTree(tree: DisjointSets[Hash]): Int =  // don't replace the postgres DB
  {
    val no = tree.elements.size - startTableSize // return the number of new elements in the union-find structure
    table.close // but don't forget to flush
    saveAddresses
    no
  }

  lazy val addressBuffer: Map[Hash, Hash] = Map()

  def insertAddress(s: (Hash, Hash)) =
  {
    addressBuffer += s

    if (addressBuffer.size >= populateTransactionSize)
      saveAddresses
  }

  def saveAddresses = {
    val convertedVector = addressBuffer map (p => (hashToArray(p._1), hashToArray(p._2)))

    try{
      DB withSession (addresses.insertAll(convertedVector.toSeq:_*)(_))
    }
    catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }

    addedAds += addressBuffer.size

    addressBuffer.clear
  }
}


