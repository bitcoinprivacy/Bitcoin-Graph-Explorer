
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
  def params = MainNetParams.get
  
  private lazy val loader = {
    val context = new Context(params)
    new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList)
  }

  lazy val blockStore = new MemoryBlockStore(params);
  lazy val chain = new BlockChain(params, blockStore);
  lazy val peerGroup = new PeerGroup(params, chain);

  lazy val addr = new PeerAddress(InetAddress.getLocalHost(), params.getPort());
  
  lazy val lines = scala.io.Source.fromFile(blockHashListFile).getLines.drop(blockCount).take(1000)
  override def blockSource = {
    peerGroup.start();
    peerGroup.addAddress(addr);
    peerGroup.waitForPeers(1).get();
    val peer = peerGroup.getConnectedPeers().get(0);

    val x = for (line <- lines) yield {
      val blockHash = Sha256Hash.wrap(line.toLowerCase);
      val future = peer.getBlock(blockHash);
      System.out.println("Waiting for node to send us the requested block: " + blockHash);
      future.get();
    }

    

    x
  }

  def stop = {
    peerGroup.stopAsync();
  }
}
