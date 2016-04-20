/**
 * Created by yzark on 12/16/13.
 */

import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import org.bitcoinj.core._
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.store._
import org.bitcoinj.utils.BlockFileLoader
import scala.collection.mutable.ArrayBuffer
import org.iq80.leveldb.impl.Iq80DBFactory.factory

package object util 
{
  lazy val conf = ConfigFactory.load()
  
  lazy val closureTransactionSize = conf.getInt("closureTransactionSize")
  lazy val closureReadSize = conf.getInt("closureReadSize")
  lazy val populateTransactionSize = conf.getInt("populateTransactionSize")
  lazy val balanceTransactionSize = conf.getInt("balanceTransactionSize")
  lazy val blockHashListFile= conf.getString("blockHashListFile")
  lazy val dustLimit = conf.getLong("dustLimit")

  lazy val blockStoreFile = new java.io.File(conf.getString("levelDBFile"))
  lazy val lockFile = conf.getString("lockFile")

  def params = MainNetParams.get

  lazy val blockStore = new LevelDBBlockStore(new Context(params), blockStoreFile, factory)
  lazy val chain = new BlockChain(params, blockStore)
  lazy val peerGroup = new PeerGroup(params, chain)
  lazy val loader = {
    new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList)
  }
  lazy val addr = new PeerAddress(InetAddress.getLocalHost(), params.getPort());
   
  def startBitcoinJ: Unit = {
    println("DEBUG: starting peergroup")
    peerGroup.start();
    peerGroup.addAddress(addr);
    peerGroup.waitForPeers(1).get();
    peerGroup.startBlockChainDownload(new DownloadProgressTracker)
  }

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
}

