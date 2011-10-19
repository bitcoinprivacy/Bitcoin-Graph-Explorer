import com.google.bitcoin.core._
import com.google.bitcoin.discovery.DnsDiscovery
import com.google.bitcoin.store.BoundedOverheadBlockStore
import java.io.File


object BlockChainDownloader {

  val params = NetworkParameters.testNet
  val wallet = new Wallet(params)
  val blockStore = new BoundedOverheadBlockStore(params, new File("bitcoin.blockchain"));
  val chain  = new BlockChain(params, wallet, blockStore)
  val peers = new PeerGroup(blockStore,params,chain)
  val listener: DownloadListener = new DownloadListener

// Load the block chain, if there is one stored locally.
  def apply()
  {
    println("Reading block store from disk");

    peers.addPeerDiscovery(new DnsDiscovery(params))
    peers.start()

    peers.startBlockChainDownload(listener)
  }
}



object Main extends App
{
  BlockChainDownloader()

}