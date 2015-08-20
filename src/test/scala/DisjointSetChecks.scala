import scala.collection.immutable.Set
import util.DisjointSets
import org.scalacheck._
import Arbitrary._
import Prop.{forAll, BooleanOperators}


object DSSpecification extends Properties("DisjointSets") {


  def isDisjoint(s: Set[Set[Int]]) =
    s.map(_.size).sum == (s.fold(Set.empty)(_ ++ _)).size

  def setOfPairsGen = for {
    s <- arbitrary[Set[Int]]
    part <- Gen.choose(1,s.size)
    e <- s
  } yield (part, e)

  def setOfPairsToDisjointSet(pairs: Set[(Int, Int)]) : Set[Set[Int]] =
    pairs.groupBy(_._1).values.toSet map ((p:Set[(Int, Int)]) => (p map (q => q._2)))

  property("Sets of nonempty disjoint Sets can be transformed into union/find structures and back") =
    forAll(setOfPairsGen) { (pairs: Set[(Int,Int)]) =>
      val set = setOfPairsToDisjointSet(pairs)
       (set == new DisjointSets(set).asSet)
    }

  property("operation find works as expected") = {
    forAll(setOfPairsGen) { pairs =>
      forAll{ (i: Int) =>
        val set = setOfPairsToDisjointSet(pairs)
        val uf = new DisjointSets(set)
        val spec = new DisjointSetsSpec[Int]
        spec.set = set

        val (_, nextSpec) = spec.find(i)
        val nextSet = nextSpec.set
        val (repOpt,nextUf) = uf.find(i)

        nextUf.asSet == nextSet && {
          (set.find(_.contains(i)), repOpt) match {
            case (None, None) => true
            case (Some(s), Some(j)) => s.contains(j)
            case _ => false
          }

        }
      }
    }
  }

  property("operation add works as expected") = {
    forAll(setOfPairsGen) { pairs =>
      forAll{ (i: Int) =>
        val set = setOfPairsToDisjointSet(pairs)
        val uf = new DisjointSets(set)
        val spec = new DisjointSetsSpec[Int]
        spec.set = set
        val nextSpec = spec.add(i)
        val nextSet = nextSpec.set
        val nextUf = uf.add(i)

        nextUf.asSet == nextSet
      }
    }
  }

  property("operation union works as expected") = {
    forAll(setOfPairsGen) { pairs =>
      forAll{ (i: Int, j: Int) =>
        val set = setOfPairsToDisjointSet(pairs)
        val uf = new DisjointSets(set)
        val spec = new DisjointSetsSpec[Int]
        spec.set = set
        val nextSpec = spec.union(i,j)
        val nextSet = nextSpec.set
        val nextUf  = uf.union(i,j)

        nextUf.asSet == nextSet
        }
      }
    }

  property("finding can exactly recreate the structure of the disjoint Sets") = {
    forAll(setOfPairsGen) { pairs =>
      val setOfSets = setOfPairsToDisjointSet(pairs)
      val unionSet = setOfSets.fold(Set.empty)(_ ++ _)

      val uf = new DisjointSets(setOfSets)
      setOfSets == unionSet.groupBy(uf.find(_)._1).values.toSet

    }

  }


}
