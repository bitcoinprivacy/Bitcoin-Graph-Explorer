
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


      def addressesPerTxQuery(tx: Column[Array[Byte]]) =
      {
        transactionDBSession {
          movements.filter(_.transaction_hash === tx).map(_.address)
        }
      }

      val addressesPerTx = Compiled(addressesPerTxQuery _)

    def txListQuery(start: Column[Array[Byte]], end: Column[Array[Byte]]) = {
      transactionDBSession {
        movements.filter(p => p.transaction_hash > start && p.transaction_hash <= end && p.index===0).map(_.transaction_hash)
       // select count(*) from movements where transaction_hash > X'ffa000' and transaction_hash < X'ffb000' and `index` = 0;
      }
    }
      val txList = Compiled(txListQuery _)


    println("running groupBy")
      var tree: util.DisjointSets[util.Hash] = new DisjointSets[Hash](scala.collection.immutable.HashMap.empty)

      for (i <- 0 until 65535 by 16){
        val start = intToByteArray(i)
        val end = intToByteArray(i+16)
        println(Hash(start) + " to " + Hash(end))
        val a = System.currentTimeMillis
        val txs = transactionDBSession {txList(start, end).list}
        val dA = System.currentTimeMillis - a
        val b = System.currentTimeMillis
        val step = txs.length
        var counter = 0
        val addressesPerTxList = {
          transactionDBSession {
            for (tx <- txs)
            yield{
               addressesPerTx(tx).run

            }
          }
        }

        println(addressesPerTxList.map(_.length).sum)
        val hashList = addressesPerTxList map (_ map (Hash(_)))
        val dB = System.currentTimeMillis - b
        val c = System.currentTimeMillis

        tree = hashList.foldLeft(tree)(
          (t,l) => {
            insertInputsIntoTree(l,t)
          }
        )

        val dC = System.currentTimeMillis - c
        val average: Double  = (dA+dB+0.0)/step
        println("["+average+"]["+dA+"]["+dB+"]["+dC+"] Done " + step + " elements")
      }

      tree
  }
}




