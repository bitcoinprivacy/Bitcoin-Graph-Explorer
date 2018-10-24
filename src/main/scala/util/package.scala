/**
 * Created by yzark on 12/16/13.
 */

import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import org.bitcoinj.core._
import org.bitcoinj.params._
import org.bitcoinj.store._
import scala.collection.mutable.ArrayBuffer
import com.typesafe.scalalogging._
import org.slf4j.LoggerFactory;

package object util 
{
  lazy val conf = ConfigFactory.load()
  lazy val networkMode = conf.getString("network")  
  lazy val closureTransactionSize = conf.getInt("closureTransactionSize")
  lazy val closureReadSize = conf.getInt("closureReadSize")
  lazy val populateTransactionSize = conf.getInt("populateTransactionSize")
  lazy val balanceTransactionSize = conf.getInt("balanceTransactionSize")
  lazy val blockHashListFile= conf.getString("blockHashListFile")
  lazy val dustLimit = conf.getLong("dustLimit")
  lazy val dataDir = conf.getString( "dataDir")
  lazy val richlistSize = conf.getInt("richlistSize")
  lazy val resumeBlockSize = conf.getInt("resumeBlockSize")
  lazy val balanceUpdateLimit = conf.getInt("balanceUpdateLimit")
  lazy val blockStoreFile = new java.io.File(conf.getString("levelDBFile"))
  lazy val lockFile = conf.getString("lockFile")
  lazy val checkUTXOsSize = conf.getInt("checkUTXOsSize")
  lazy val internetAddress = conf.getString("bitcoin.ip") match {
    case "localhost" => 
      InetAddress.getLocalHost()
    case e: String =>
      InetAddress.getByName(e)
  }
  lazy val maxPopulate = conf.getInt("bitcoin.maxPopulate")

  def params = 
    if (networkMode == "main")
      MainNetParams.get
    else if (networkMode == "testnet")
      TestNet3Params.get
    else
      RegTestParams.get

  val log = Logger(LoggerFactory.getLogger("bge"))

  lazy val blockStore = new LevelDBBlockStore(new Context(params), blockStoreFile) //, factory)
  lazy val chain = new BlockChain(params, blockStore)
  lazy val peerGroup = new PeerGroup(params, chain)
  lazy val addr = new PeerAddress(params, internetAddress, params.getPort());

  def startBitcoinJ: Unit = {
    log.info("starting peergroup")
    peerGroup.start
    peerGroup.addAddress(addr)
    peerGroup.waitForPeers(1).get()
    peerGroup.getDownloadPeer.setDownloadParameters(Long.MaxValue, false) // only download headers
    peerGroup.downloadBlockChain
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

  // convert case class to map.
  def getCCParams(cc: AnyRef) =
    (Map[String, Any]() /: cc.getClass.getDeclaredFields) {(a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
    }

}
