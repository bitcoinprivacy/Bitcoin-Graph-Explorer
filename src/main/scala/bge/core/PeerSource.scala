
package core

import util._

trait PeerSource extends BlockSource {
  
  lazy val truncated = getCurrentLongestChainFromBlockCount take resumeBlockSize 
  
  override def blockSource =
  {
    val peer = peerGroup.getConnectedPeers().get(0);
    for ((_,end) <- truncated.lastOption)
      log.info("reading blocks from " + blockCount + " to " + end)

    for ((blockHash,no) <- truncated.toIterator) yield {
      val future = peer.getBlock(blockHash)
      log.info("Waiting for node to send us the requested block: " + blockHash)
      val block = future.get()
      log.info("Block received")
      (block,no)
    }

  }

}
