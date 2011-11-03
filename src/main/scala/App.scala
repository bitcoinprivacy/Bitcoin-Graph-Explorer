import annotation.tailrec
import com.google.bitcoin.core._
import com.google.bitcoin.discovery.{IrcDiscovery, SeedPeers, DnsDiscovery}
import com.google.bitcoin.store.BoundedOverheadBlockStore
import java.io.File
import java.net.InetSocketAddress

object Main extends App {

  val params = NetworkParameters.testNet
  val wallet = new Wallet(params)
  val blockStore = new BoundedOverheadBlockStore(params, new File("bitcoin.blockchain"));
  val chain = new BlockChain(params, wallet, blockStore)
  val peer = new Peer(params,new PeerAddress(new InetSocketAddress("localhost",8333)),chain)

  object BlockChainDownloader {


    // Load the block chain, if there is one stored locally.
    def apply() {
      println("Reading block store from disk");

      peer.connect()
      peer.run()

      peer.startBlockChainDownload()
    }
  }

  object Graph {

    var latestknownhash = params.genesisBlock.getHash


    @tailrec def invertedToDoListOfHashes(block: StoredBlock, list: List[Sha256Hash]): List[Sha256Hash] = {
      val hash = block.getHeader.getHash
      if (hash == latestknownhash) list
      else invertedToDoListOfHashes(block.getPrev(blockStore), hash :: list)
    }

    def update() { // updates the Graph to reflect the known blockchain

      val toDo = invertedToDoListOfHashes(chain.getChainHead, List())

    }

  }

  BlockChainDownloader()
  Graph.update()

}