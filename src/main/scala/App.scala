import annotation.tailrec
import com.google.bitcoin.core._
import com.google.bitcoin.discovery.DnsDiscovery
import com.google.bitcoin.store.BoundedOverheadBlockStore
import java.io.File

object Main extends App {

  val params = NetworkParameters.prodNet
  val wallet = new Wallet(params)
  val blockStore = new BoundedOverheadBlockStore(params, new File("bitcoin.blockchain"));
  val chain = new BlockChain(params, wallet, blockStore)
  val peers = new PublicPeerGroup(blockStore, params, chain)
  val listener: DownloadListener = new DownloadListener

  object BlockChainDownloader {


    // Load the block chain, if there is one stored locally.
    def apply() {
      println("Reading block store from disk");

      peers.addPeerDiscovery(new DnsDiscovery(params))
      peers.start()

      peers.startBlockChainDownload(listener)
    }
  }

  object Graph {

    var latestknownhash = params.genesisBlock.getHash
    var toDo = List[Sha256Hash]()

    @tailrec def invertedToDoListOfHashes(block: StoredBlock, list: List[Sha256Hash]): List[Sha256Hash] = {
      if (block==null) return list  // catch null-case from BitcoinJ
      val hash = block.getHeader.getHash
      if (hash == latestknownhash) list
      else invertedToDoListOfHashes(block.getPrev(blockStore), hash :: list)
    }

    def update() { // updates the Graph to reflect the known blockchain

     toDo = invertedToDoListOfHashes(chain.getChainHead, List())

    }

  }

  BlockChainDownloader()
  Graph.update()
  println("update ready")

}