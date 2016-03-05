
package core

import org.bitcoinj.core._
import scala.collection.convert.WrapAsScala._
import util._

trait PeerSource extends BlockSource {
  
  lazy val truncated = getCurrentLongestChainFromBlockCount dropRight 5
  
  override def blockSource = {
    
    val peer = peerGroup.getConnectedPeers().get(0);
    for ((_,end) <- truncated.lastOption)
      println("reading blocks from " + blockCount + " to " + end)

    for ((blockHash,no) <- truncated.toIterator) yield {
      val future = peer.getBlock(blockHash);
      System.out.println("Waiting for node to send us the requested block: " + blockHash + " at " + java.util.Calendar.getInstance().getTime());
      val block = future.get();
      System.out.println("Block received at " + java.util.Calendar.getInstance().getTime())
      (block,no)
    }

  }

}
