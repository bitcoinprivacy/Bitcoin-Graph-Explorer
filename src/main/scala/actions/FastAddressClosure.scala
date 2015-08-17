package actions

import core._
import java.util.Calendar
import scala.slick.driver.JdbcDriver.simple._
import scala.slick.jdbc._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._

object FastAddressClosure extends AddressClosure with BitcoinDB {
  def generateTree =
  {
    val step = conf.getInt("closureReadSize")
    def txListQuery(block: Column[Int]) = {
      val emptyArray = Hash.zero(0).array.toArray

      transactionDBSession {
        for (q <- movements.filter(_.height_out >= block).filter(_.height_out < block+step).filter(_.address =!= emptyArray))
          yield (q.spent_in_transaction_hash, q.address)
        // in order to read quickly from db, we need to read in the order of insertion
      }
    }
    val txList = Compiled(txListQuery _)

    def addBlock(block: Int, tree: DisjointSets[Hash]): DisjointSets[Hash] = {

      println("reading " + step + " blocks from " + block + " " + Calendar.getInstance().getTime())
      val txAndAddressList = transactionDBSession { txList(block).run.toVector }
      val addressesPerTxMap = txAndAddressList.groupBy(_._1)
      val hashList = addressesPerTxMap.values map (_ map (p=>Hash(p._2)))

      println("folding and merging " + Calendar.getInstance().getTime() )
      hashList.foldLeft (tree) ((t,l) => insertInputsIntoTree(l,t))
    }

    val table: LmdbMap = LmdbMap.create("closures")
    (0 until blockCount by step).foldRight (new DisjointSets[Hash])(addBlock)
   }
}


