package actions

import core._
import java.util.Calendar
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._

class ResumeClosure(blockHeights: Vector[Int]) extends AddressClosure(blockHeights: Vector[Int]) {

  val table = LmdbMap.open("closures")
  override lazy val unionFindTable = new ClosureMap(table)

  override def insertInputsIntoTree(addressList: Iterable[Hash], tree: DisjointSets[Hash]): DisjointSets[Hash] =
  {
    val addedTree = addressList.foldLeft(tree)((t:DisjointSets[Hash],a:Hash) => t.add(a))
    val tree2 = addedTree.union(addressList)

    // update the postgres DB

    // must be at least 2 addresses
    val (representantOpt,result) = tree2 find (addressList.head)
    val newRep = representantOpt.get

    transactionDBSession {
      val byAddress = addresses.findBy(_.hash) // TODO: Compile?

      val foundRepresentantOption = byAddress(newRep.array.toArray).firstOption

      for (address <- addressList)
        byAddress(address.array.toArray).firstOption match
        {
          case None => addresses.insert(address,newRep)         //insert into DB, TODO: maybe bulk insert
          case Some((_,oldRep)) if (oldRep != newRep) => // if representant new, update everything that had the old one
            val updateQuery = for(p <- addresses if p.representant === oldRep) yield p.representant
            updateQuery.update(newRep) // TODO: compile query
          case _ => // nothing to do

      }
    }

    result
  }

  override def saveTree(tree: DisjointSets[Hash]): Int =  // don't replace the postgres DB
  {
    table.commit // but don't forget to flush
    0
  }

}


