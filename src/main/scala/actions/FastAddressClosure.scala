package actions

import core._
import java.util.Calendar
import scala.slick.driver.JdbcDriver.simple._
import scala.slick.jdbc._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._

class FastAddressClosure(table: LmdbMap, blockHeights: Vector[Int]) extends AddressClosure with BitcoinDB {

  def generateTree =
  {
    val step = conf.getInt("closureReadSize")
    def txListQuery(blocks: Seq[Int]) = {
      val emptyArray = Hash.zero(0).array.toArray

      transactionDBSession {
        for (q <- movements.filter(_.height_out inSet blocks).filter(_.address =!= emptyArray))
        yield (q.spent_in_transaction_hash, q.address)
        // in order to read quickly from db, we need to read in the order of insertion
      }
    }
  //  val txList = Compiled(txListQuery _)

    def addBlock(block: Int, tree: DisjointSets[Hash]): DisjointSets[Hash] = {
      val blocks = blockHeights.slice(block,block+step)
      println("reading block " + block + " " + Calendar.getInstance().getTime())
      val txAndAddressList = transactionDBSession { txListQuery(blocks).run.toVector }
      val addressesPerTxMap = txAndAddressList.groupBy(p=>Hash(p._1))
      println("===================")
//      println(addressesPerTxMap)
      val hashList = addressesPerTxMap.values map (_ map (p=>Hash(p._2)))
//      println(hashList)
      val nontrivials = hashList filter (_.length > 1)
      println("folding and merging " + nontrivials.size + Calendar.getInstance().getTime() )

      nontrivials.foldLeft (tree) ((t,l) => insertInputsIntoTree(l,t))
    }


    val adapter: ClosureMap = new ClosureMap(table)
    val result = (0 until blockHeights.length by step).foldRight(new DisjointSets[Hash](adapter))(addBlock)
    table.commit
    println("finished generation")
    result
   }
}


