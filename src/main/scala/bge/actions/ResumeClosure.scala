package actions

import core._
import db._
import util._
import Hash._

class ResumeClosure(blockHeights: Vector[Int], changedAds: Map[Hash,Long]) extends AddressClosure(blockHeights: Vector[Int]) with BitcoinDB {

  lazy val table = LmdbMap.open("closures")
  override lazy val unionFindTable = new ClosureMap(table)

  lazy val changedAddresses = changedAds.keys.toSet - Hash.zero(0)
  lazy val oldReps = getAddressReps(changedAddresses) // only read from addresses once

  lazy val (finalTree, touchedReps) = // a map of new reps to sets of their addresses that have been touched
    changedAddresses.foldLeft((generatedTree,Map[Hash,Set[Hash]]())){
      case ((tree,tR),address) =>
        val (repOpt, ntree) = tree.find(address)
        (ntree, repOpt match {
           case None => tR + (address -> Set(address))
           case Some (r) => tR + (r -> (tR.getOrElse(r, Set())+address))
         })
    }
  // this includes addresses that are not yet in the tree! otherwise, the below would be easier
  //  streamOfUnionLists.flatten.flatten.toSet.groupBy(generatedTree.onlyFind)
  
  // a map of new reps to the old reps of these addresses, or the addresses themselves as their new rep if they weren't here before
  lazy val changedReps = for { (rep,ads) <- touchedReps }
                            yield (rep, ads.map(a => oldReps.getOrElse(a,a)))

  lazy val toSave = for {(rep,ads) <- touchedReps.toSeq
                         ad <- ads
                         if ! oldReps.contains(ad)
  }
  yield (ad,rep)

  lazy val addedAds = toSave.size

  lazy val addedReps = touchedReps.count {  // the number of reps that were not reps before
    case (rep,_) => ! oldReps.exists{case (ad,oldR) => rep==oldR}
  }

  lazy val deletedReps = changedReps.values.flatten.toSet.filter(p => !changedReps.contains(p) && oldReps.values.toSet.contains(p)).size

  lazy val addedClosures = addedReps - deletedReps

  override def saveTree: Int =  // don't replace the postgres DB
  {
    val no = finalTree.elements.size - startTableSize // return the number of new elements in the union-find structure

    updateAddressDB(changedReps)
    saveAddresses(toSave)

    table.close // but don't forget to flush

    no
  }

}


