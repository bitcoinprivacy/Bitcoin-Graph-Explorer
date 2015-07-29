
package actions

import scala.collection.mutable.ArrayBuffer
import scala.slick.jdbc._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._
import core._

object FastAddressClosure extends AddressClosure {
  def generateTree =
  {
    def intToByteArray(x:Int): Array[Byte] = {
      val buf = new ArrayBuffer[Byte](2)
      for(i <- 0 until 2) {
        buf += ((x >>> (2 - i - 1 << 3)) & 0xFF).toByte
      }
      buf.toArray
    }

    def txListQuery(start: Column[Array[Byte]], step: ConstColumn[Long]) = {
      transactionDBSession {
        movements.filter(p => p.transaction_hash >= start).sortBy(_.transaction_hash.asc).take(step).map(p => (p.transaction_hash,p.address))

        // select hex(address) from movements where transaction_hash > X'abc1' order by transaction_hash asc limit 16000; mysql stupidity workaround
      }
    }
      val txList = Compiled(txListQuery _)

    val step = 20000


    @annotation.tailrec
    def addNextRows(start: Array[Byte], tree: DisjointSets[Hash]): DisjointSets[Hash] = {
      println("reading " + step + " elements from " + Hash(start))
      val txAndAddressList = transactionDBSession { txList(start,step).run.toVector }
      txAndAddressList.lastOption match
      {
        case None => tree
        case Some((newStart,_)) =>

          val addressesPerTxList = txAndAddressList.groupBy(_._1) - newStart
          // remove last tx from list
          val hashList = addressesPerTxList.values map (_ map (p=>Hash(p._2)))
          println("folding and merging")
          val newTree = {
            hashList.foldLeft(tree)(
              (t,l) => {
                insertInputsIntoTree(l,t)
              }
            )
          }
          addNextRows(newStart, newTree)
      }

    }


    addNextRows(Hash.zero(1).array.toArray, new DisjointSets[Hash](collection.immutable.HashMap.empty))
   }
}


