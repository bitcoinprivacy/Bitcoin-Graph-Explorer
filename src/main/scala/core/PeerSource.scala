
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
  val params = MainNetParams.get
  val context = new Context(params)
  private val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList)

  val blockStore = new MemoryBlockStore(params);
  val chain = new BlockChain(params, blockStore);
  val peerGroup = new PeerGroup(params, chain);
  peerGroup.start();
  val addr = new PeerAddress(InetAddress.getLocalHost(), params.getPort());
  peerGroup.addAddress(addr);
  peerGroup.waitForPeers(1).get();
  val peer = peerGroup.getConnectedPeers().get(0);

  val lines = scala.io.Source.fromFile(blockHashListFile).getLines.drop(blockCount)
  override def blockSource = {
    val x = for (line <- lines) yield {
      val blockHash = Sha256Hash.wrap(line.toLowerCase);
      val future = peer.getBlock(blockHash);
      System.out.println("Waiting for node to send us the requested block: " + blockHash);
      future.get();
    }
    //peerGroup.stopAsync();
    x
  }
}
