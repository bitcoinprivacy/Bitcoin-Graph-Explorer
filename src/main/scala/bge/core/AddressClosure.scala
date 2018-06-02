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
  var addedAds = 0
  var addedReps = 0

  var temp = new ClosureMap(Map.empty)
  var changedReps = new DisjointSets[Hash](temp)
  def saveTree(tree: DisjointSets[Hash]): Int

  def generateTree: DisjointSets[Hash] =
  {
    // TODO: Same problem as with BlockReader, there we used a function "pre" to initialize the values. 
    temp = new ClosureMap(Map.empty)
    changedReps = new DisjointSets[Hash](temp)
    addedAds = 0;
    addedReps = 0;
    def addBlocks(startIndex: Int, tree: DisjointSets[Hash]): DisjointSets[Hash] = {
      val blocks = blockHeights.slice(startIndex,startIndex+closureReadSize)
      val blockNo = blocks.head
      val txAndAddressList = txListQuery(blocks)
      val addressesPerTxMap = txAndAddressList.groupBy(p=>Hash(p._1))
      val hashList = addressesPerTxMap.values map (_ map (p=>Hash(p._2)))
      val nonTrivials = hashList filter (_.length > 1)
      val result = nonTrivials.foldLeft (tree) ((t,l) => insertInputsIntoTree(l,t))
      log.info("reading " + blocks.length + " blocks from " + blockNo)
      result
    }

    val result = (0 until blockHeights.length by closureReadSize).foldRight(new DisjointSets[Hash](unionFindTable))(addBlocks)
    log.info("finished generation")
    result
  }

  def insertInputsIntoTree(addresses: Iterable[Hash], tree: DisjointSets[Hash]): DisjointSets[Hash] =
  {
    val addedTree = addresses.foldLeft(tree)((t:DisjointSets[Hash],a:Hash) => t add a)
    addedTree.union(addresses)
  }

  log.info("applying closure ")
  val timeStart = System.currentTimeMillis
  val startTableSize = unionFindTable.size
  val countSave = saveTree(generateTree)

  val totalTime = System.currentTimeMillis - timeStart

  log.info("Total of %s addresses added to closures in %s s, %s µs per address" format
    (countSave, totalTime / 1000, 1000 * totalTime / (countSave + 1)))
}


