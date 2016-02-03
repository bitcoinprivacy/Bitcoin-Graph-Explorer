
package core

import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BlockFileLoader
import org.bitcoinj.core._
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.MemoryBlockStore;
import java.net.InetAddress
import util._
import scala.collection.convert.WrapAsScala._

// In java that should be implements libs.BlockSource
trait PeerSource extends BlockSource {
  
  //lazy val lines = scala.io.Source.fromFile(blockHashListFile).getLines.drop(blockCount) take 100

  lazy val begin = blockCount
  lazy val lastBlock = chain.getChainHead
  lazy val lastNo = lastBlock.getHeight
  lazy val lines = (begin to lastNo).foldRight((lastBlock,Vector[(Int,Sha256Hash)]())){
      case (no,(bl,vec)) => (bl.getPrev(blockStore),(no,bl.getHeader.getHash)+:vec)}._2
  lazy val truncated = lines take Math.min(100,Math.max(lines.length-5,0)) 
  
  override def blockSource = {
    
    val peer = peerGroup.getConnectedPeers().get(0);
    for ((end,_) <- truncated.lastOption)
      println("reading blocks from " + begin + " to " + end)

    for ((no,blockHash) <- truncated.toIterator) yield {
      val future = peer.getBlock(blockHash);
      System.out.println("Waiting for node to send us the requested block: " + blockHash + " at " + java.util.Calendar.getInstance().getTime());
      val block = future.get();
      System.out.println("Block received at " + java.util.Calendar.getInstance().getTime())
      (block,no)
    }

  }

  
}
