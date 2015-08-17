/**
 * Created by yzark on 12/16/13.
 */

import com.typesafe.config.ConfigFactory
import core._
import scala.slick.driver.JdbcDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util.LmdbMap
import collection.mutable.ArrayBuffer

package object util extends BitcoinDB
{
  val conf = ConfigFactory.load()

  var closureTransactionSize = conf.getInt("closureTransactionSize")
  var closureReadSize = conf.getInt("closureReadSize")
  var populateTransactionSize = conf.getInt("populateTransactionSize")
  var balanceTransactionSize = conf.getInt("balanceTransactionSize")
  var blockHashListFile= conf.getString("blockHashListFile")
  var dustLimit = conf.getLong("dustLimit")

  println(blockHashListFile)

  val arrayNull = Hash.zero(1).array.toArray


  def getLongestBlockChainHashSet: Map[Hash,Int] =
  {
    val lines = scala.io.Source.fromFile(blockHashListFile).getLines
    val hashes = for (line <- lines) yield Hash(line)
    hashes.zipWithIndex.toMap
  }



  def toArrayBuf[A:IntOrLong](x:A, INTBYTES:Int = 4)(implicit f:IntOrLong[A]): ArrayBuffer[Byte] = {
    val buf = new ArrayBuffer[Byte](f.length)
    for(i <- 0 until f.length) {
      buf += f.&(f.>>>(x,(f.length - i - 1 << 3)), 0xFF).toByte
    }
    buf
  }

  trait IntOrLong[A]{
    def length: Int
    def >>> : (A,Long) => A
    def & : (A, Int) => Long
  }

  object IntOrLong {
    implicit object IntWitness extends IntOrLong[Int]{
      def length=4
      def >>> = (_ >>> _)
      def & = (_ & _)
    }

    implicit object LongWitness extends IntOrLong[Long]{
      def length=8
      def >>> = (_ >>> _)
      def & = (_ & _)
    }
  }


  // just experimenting here
  //(spent_in_transaction_hash,transaction_hash,address,index,value, height_in, height_out)
 def readUTXOs: UTXOs = {
    transactionDBSession {
      val query = for ( a <- utxo.take(1000000)) yield ((a.transaction_hash, a.index),(a.address, a.value, a.block_height))
      var x = 0

      val converted = for {
        ((t,i),(a,v,b)) <- query.iterator
      }
      yield {
        println(x)
        x+=1
        (Hash(t),i) -> (Hash(a),v,b)
      }

      converted.foldLeft (new UTXOs(LmdbMap.empty): UTXOs){println("ssaving");_+=_}



    }

  }
}
