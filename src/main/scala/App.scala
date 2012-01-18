package BitcoinGraphExplorer


import com.google.bitcoin.core._
import com.google.bitcoin.discovery.DnsDiscovery
import com.google.bitcoin.store.BoundedOverheadBlockStore
import java.io.File


object Main extends App {

  val params = NetworkParameters.prodNet
  val wallet = new Wallet(params)
  val blockStore = new BoundedOverheadBlockStore(params, new File("bitcoin.blockchain"));
  val chain = new BlockChain(params, wallet, blockStore)
  val peers = new PeerGroup(blockStore, params, chain)
  val listener: DownloadListener = new Graph.NeoDownLoadListener(params)

  println("Reading block store from disk");

  peers.addPeerDiscovery(new DnsDiscovery(params))

  peers.start()

  peers.startBlockChainDownload(listener)

  // hook up a DownloadListener to PeerGroup
  // this gets called every time a new block is linked to the chain with the block (this block is not castrated before we get to it)

  // there is a possible problem here: we only get called if the received block can be immediately attached to the chain.
  // otherwise, it is added to blockChain.unconnectedBlocks, and only potentially removed from there when another block is
  // successfully added (which we get noticed of). This might still work in practice, but should be tested:
  // whenever we hear of a block, we should already know its predecessor. If we don't, we have missed something.
  // Even if this never happens, we might still be one block behind the blockchain.

  // Okay, it does happen. But only at the end of a catch-up download, it seems. Projected solution:
  // todo: manually tell peer to reload missing (for us) blocks


  // persist everything, including blockhash
  // do not persist current BestChain status, because this might change. query this dynamically (todo: check if this is viable)
  // todo: on re-run with existing blockchain, check if we know of all the blocks mentioned already (otherwise, just reload)
  // todo: check what happens on fork

}