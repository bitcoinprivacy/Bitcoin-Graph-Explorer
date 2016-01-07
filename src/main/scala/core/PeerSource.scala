
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
  lazy val end = chain.getBestChainHeight() - 5
  lazy val range = (begin until end) take 100
  lazy val lines = for (no <- range)
                   yield (chain.getHeightFuture(no),no)

  override def blockSource = {
    start
    
    val peer = peerGroup.getConnectedPeers().get(0);
    println("reading blocks from " + begin + " until " + end)

    

    for ((line,no) <- lines.toIterator) yield {
      val blockHash = line.get.getHeader.getHash
      val future = peer.getBlock(blockHash);
      System.out.println("Waiting for node to send us the requested block: " + blockHash + " at " + java.util.Calendar.getInstance().getTime());
      val block = future.get();
      System.out.println("Block received at " + java.util.Calendar.getInstance().getTime())
      (block,no)
    }

    

  }

  
}
