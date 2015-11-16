package core

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/13
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
import util._
import java.io._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.Map
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import java.lang.System
import java.util.Calendar

abstract class AddressClosure(blockHeights: Vector[Int])
{
  lazy val unionFindTable: Map[Hash,(Int,Hash)] = Map.empty

  def saveTree(tree: DisjointSets[Hash]): Int

  def generateTree: DisjointSets[Hash] =
  {
    val step = conf.getInt("closureReadSize")

    def addBlocks(startIndex: Int, tree: DisjointSets[Hash]): DisjointSets[Hash] = {
      val blocks = blockHeights.slice(startIndex,startIndex+step)
      for (blockNo <- blocks.headOption)
        println("reading " + blocks.length + " blocks from " + blockNo + " at " + Calendar.getInstance().getTime())
      val txAndAddressList = transactionDBSession { txListQuery(blocks).run.toVector }
      val addressesPerTxMap = txAndAddressList.groupBy(p=>Hash(p._1))
      val hashList = addressesPerTxMap.values map (_ map (p=>Hash(p._2)))
      val nontrivials = hashList filter (_.length > 1)

      println("folding and merging " + nontrivials.size + Calendar.getInstance().getTime())
      nontrivials.foldLeft (tree) ((t,l) => insertInputsIntoTree(l,t))
    }

    val result = (0 until blockHeights.length by step).foldRight(new DisjointSets[Hash](unionFindTable))(addBlocks)
    println("finished generation")
    result
  }

  def insertInputsIntoTree(addresses: Iterable[Hash], tree: DisjointSets[Hash]): DisjointSets[Hash] =
  {
    val addedTree = addresses.foldLeft(tree)((t:DisjointSets[Hash],a:Hash) => t add a)
    addedTree.union(addresses)
  }

  println("applying closure ")
  val timeStart = System.currentTimeMillis
  val startTableSize = unionFindTable.size
  val countSave = saveTree(generateTree)

  val totalTime = System.currentTimeMillis - timeStart

  println("DONE: Total of %s addresses added to closures in %s s, %s µs per address" format
    (countSave, totalTime / 1000, 1000 * totalTime / (countSave + 1)))
}


