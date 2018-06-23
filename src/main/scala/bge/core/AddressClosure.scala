package core

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/13
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
import util._
import scala.slick.driver.PostgresDriver.simple._
import scala.collection.mutable.Map
import java.lang.System

abstract class AddressClosure(blockHeights: Vector[Int]) extends db.BitcoinDB
{
  lazy val unionFindTable: Map[Hash,(Int,Hash)] = Map.empty

  // def unions(blocks: Vector[Int]): Iterable[Seq[Hash]] = {
  //   val txAndAddressList = txListQuery(blocks)
  //   val addressesPerTxMap = txAndAddressList.groupBy(p=>Hash(p._1))
  //   val hashList = addressesPerTxMap.values map (_ map (p=>Hash(p._2)))
  //   hashList // filter (_.length > 1)
  // }

  // lazy val streamOfUnionLists: Stream[Iterable[Seq[Hash]]] = (0 until blockHeights.length by closureReadSize).toStream map startIndexToUnions

  // def startIndexToUnions(startIndex: Int): Iterable[Seq[Hash]] = {
  //   val blocks = blockHeights.slice(startIndex,startIndex+closureReadSize)
  //   val blockNo = blocks.head
  //   log.info("Closure working on " + blocks.length + " blocks from " + blockNo)
  //   unions(blocks)
  // }

  lazy val generatedTree: DisjointSets[Hash] =
  {
    def addBlocks(startIndex: Int, tree: DisjointSets[Hash]): DisjointSets[Hash] = {
      val blocks = blockHeights.slice(startIndex,startIndex+closureReadSize)
      val blockNo = blocks.head
      val txAndAddressList = txListQuery(blocks)
      val addressesPerTxMap = txAndAddressList.groupBy(p=>Hash(p._1))
      val hashList = addressesPerTxMap.values map (_ map (p=>Hash(p._2)))
      // val nonTrivials = hashList filter (_.length > 1) // premature optimization
      val result = hashList.foldRight (tree)(insertInputsIntoTree)
      log.info("Closured " + blocks.length + " blocks from " + blockNo)
      result
    }

    (0 until blockHeights.length by closureReadSize).foldRight(new DisjointSets[Hash](unionFindTable))(addBlocks)
  }

  def saveTree: Int

  // lazy val generatedTree: DisjointSets[Hash] =
  //   streamOfUnionLists.foldRight(new DisjointSets[Hash](unionFindTable)){
  //     case (unions,tree) =>
  //       unions.foldRight(tree)(insertInputsIntoTree)
  //   }

  def insertInputsIntoTree(addresses: Iterable[Hash], tree: DisjointSets[Hash]): DisjointSets[Hash] =
  {
    val addedTree = addresses.foldLeft(tree)((t:DisjointSets[Hash],a:Hash) => t add a)
    addedTree.union(addresses)
  }

  val timeStart = System.currentTimeMillis
  val startTableSize = unionFindTable.size
  val countSave = saveTree
  val totalTime = System.currentTimeMillis - timeStart

  log.info("Total of %s addresses added to closures in %s s, %s µs per address" format
    (countSave, totalTime / 1000, 1000 * totalTime / (countSave + 1)))
}


