/**
 * Created by yzark on 12/16/13.
 */

import com.typesafe.config.ConfigFactory
import core._
import scala.slick.driver.JdbcDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession


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


  // just experimenting here
  //(spent_in_transaction_hash,transaction_hash,address,index,value, height_in, height_out)
 def readUTXOs: UTXOs = {
    transactionDBSession {
      val query = for ( a <- utxo.sortBy(p => (p.transaction_hash,p.index) )) yield ((a.transaction_hash, a.index),(a.address, a.value, a.block_height))
      var x = 0
      val converted = for { group <- query.iterator.grouped(10000)
        ((t,i),(a,v,b)) <- group
     }
      yield {
        println(x)
        x+=1
        (Hash(t),i) -> (Hash(a),v,b)
      }

      converted.foldLeft (new UTXOs(collection.immutable.HashMap.empty): UTXOs)(_+_)

    }

  }
}
