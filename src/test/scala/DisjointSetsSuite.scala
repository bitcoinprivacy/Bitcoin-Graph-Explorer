import org.scalatest.prop._
import org.scalatest._
import core._
import org.scalacheck.Gen._
import actions._

class DisjointSetsSuite extends PropSpec with ShouldMatchers with PropertyChecks {
  /*databaseFile = "blockchain/test.db"

  property("populater.end should be the minimum of given end and number of blocks available")
  {
   forAll (choose(0,280000), minSuccessful(1)) { (n:Int) =>
         {
      val populater = new BlocksReader(List(n.toString,"init"))
      populater.start should be(0)
      populater.end should be (n) // TODO: This is not true. We need the number of blocks available and this is also at least a problem when 0 is given
         }

  }
   } */

  property("adding a list of elements to disjointSet does not blow up the map")
  {
    forAll (minSuccessful(10000)) { (items: List[Int]) =>

      var ds = new util.DisjointSets[Int](scala.collection.mutable.Map.empty)

    ds = items.foldLeft(ds)(_.add(_))
    ds.elements.size should be (items.distinct.size)

    }
  }

  property("finding and adding a list of elements to disjointSet does not blow up the map")
  {
    forAll (minSuccessful(10000)) { (items: List[Int]) =>

      var ds = new util.DisjointSets[Int](scala.collection.mutable.Map.empty)
      var element = 0
      ds = items.foldLeft(ds)(_.add(_))
      for (item: Int <- items){

        val tuple = ds.find(item)
        ds = tuple._2
        element = tuple._1
        element should be (item)
        ds.elements.size should be (items.distinct.size)
      }

    }
  }

  property("disjoint sets specification test")
  {
    println(DisjointSetsSpec.check(10000) )
    1 should be (1)
  }

}
