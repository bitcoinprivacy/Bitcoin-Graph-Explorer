/**
 * Created by yzark on 12/16/13.
 */

import com.typesafe.config.ConfigFactory
import core._
import java.net.InetAddress
import org.bitcoinj.core._
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.store._
import org.bitcoinj.utils.BlockFileLoader
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable.ArrayBuffer
import scala.slick.driver.JdbcDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._

package object util extends BitcoinDB
{
  lazy val conf = ConfigFactory.load()

  lazy val closureTransactionSize = conf.getInt("closureTransactionSize")
  lazy val closureReadSize = conf.getInt("closureReadSize")
  lazy val populateTransactionSize = conf.getInt("populateTransactionSize")
  lazy val balanceTransactionSize = conf.getInt("balanceTransactionSize")
  lazy val blockHashListFile= conf.getString("blockHashListFile")
  lazy val dustLimit = conf.getLong("dustLimit")

  lazy val arrayNull = Hash.zero(1).array.toArray


  lazy val blockStoreFile = new java.io.File("/root/Bitcoin-Graph-Explorer/blockchain/spv.blockstore")

  def params = MainNetParams.get

  lazy val blockStore = new LevelDBBlockStore(new Context(params),blockStoreFile)
  lazy val chain = new BlockChain(params, blockStore);
  lazy val peerGroup = new PeerGroup(params, chain);
  lazy val loader = {
    new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList)
  }

  lazy val addr = new PeerAddress(InetAddress.getLocalHost(), params.getPort());

  def startBitcoinJ: Unit = {
    
    peerGroup.start();
    peerGroup.addAddress(addr);
    peerGroup.waitForPeers(1).get();
    peerGroup.startBlockChainDownload(new DownloadProgressTracker)
  }

  // def stopBitcoinJ = {
  //   peerGroup.stopAsync()
  // }

/*  def getLongestBlockChainHashSet: Map[Hash,Int] =
  {
    val lines = scala.io.Source.fromFile(blockHashListFile).getLines
    val hashes = for (line <- lines) yield Hash(line)
    hashes.zipWithIndex.toMap
  }
 */


  def toArrayBuf[A:IntOrLong](x:A)(implicit f:IntOrLong[A]): ArrayBuffer[Byte] = {
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

  def countLines(fileName: String) =
    scala.io.Source.fromFile(fileName).getLines.length


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
