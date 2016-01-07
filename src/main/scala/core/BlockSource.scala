package core

import org.bitcoinj.core.{Block, NetworkParameters}
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BlockFileLoader
import org.bitcoinj.core._
import org.bitcoinj.store.MemoryBlockStore
import org.bitcoinj.store.BlockStore;
import java.net.InetAddress
import util._
import scala.collection.convert.WrapAsScala._
import sys.process._

/**
 * Created by yzark on 25.08.14.
 */
trait BlockSource {
  def params = MainNetParams.get

  val numberOfHeaders = 500000
  
  lazy val blockStore = new MemoryBlockStore(params,blockStoreFile){
    //println("shit")
//    numHeaders=1000000
//    randomAccessFile.setLength(getFileSize)
  //  buffer = randomAccessFile.getChannel.map(java.nio.channels.FileChannel.MapMode.READ_WRITE, 0, getFileSize)

    override def getFileSize : Int = {
      SPVBlockStore.RECORD_SIZE * numberOfHeaders + SPVBlockStore.FILE_PROLOGUE_BYTES
    }
  }


  lazy val chain = new BlockChain(params, blockStore);
  lazy val peerGroup = new PeerGroup(params, chain);
  lazy val loader = {
    val context = new Context(params) // had to put this here because of scala trait initialization madness
    new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList)
  }

  lazy val addr = new PeerAddress(InetAddress.getLocalHost(), params.getPort());

  def start: Unit = {
    Seq("bitcoind","-daemon").run
    peerGroup.start();
    peerGroup.addAddress(addr);
    peerGroup.waitForPeers(1).get();
    
    for (block <- asScalaIterator(loader))
      chain add block
    waitIfNewBlocks(0)
  }

  def blockSource: Iterator[(Block,Int)] // block,height


  @annotation.tailrec final def waitIfNewBlocks(last: Int): Unit = {

    val waitTime = 10000
    Thread sleep waitTime
    val current = chain.getBestChainHeight

    println("waiting " + waitTime + " for bitcoinJ: last state (0 is error): " + last + ". current state:"+ current + " at " +  java.util.Calendar.getInstance().getTime())

    if (current ==  0 || ( /*current < 10000 &&*/ last != current))
      waitIfNewBlocks(current)
    else
      println("done")
  }

  def stop = {
    peerGroup.stopAsync()
  }

}
