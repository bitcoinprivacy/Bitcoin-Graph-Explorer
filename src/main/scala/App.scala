import annotation.tailrec
import com.google.bitcoin.core._
import com.google.bitcoin.discovery.DnsDiscovery
import com.google.bitcoin.store.BoundedOverheadBlockStore
import java.io.File
import java.util.HashMap
import org.neo4j.kernel.Config
import collection.JavaConversions._

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

  import org.neo4j.graphdb._
  import org.neo4j.kernel.EmbeddedGraphDatabase
  import org.neo4j.scala._
  import com.google.bitcoin.core.Transaction

  // because otherwise shadowed by neo4j

  object Graph extends Neo4jWrapper {


    val config = new HashMap[String, String]; // turn on auto-indexing
    config.put(Config.NODE_KEYS_INDEXABLE, "TransactionHash");
    // config.put( Config.RELATIONSHIP_KEYS_INDEXABLE, "HasOutputs" );
    config.put(Config.NODE_AUTO_INDEXING, "true");
    config.put(Config.RELATIONSHIP_AUTO_INDEXING, "true");

    implicit val neo: GraphDatabaseService = new EmbeddedGraphDatabase("neodb", config)

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        neo.shutdown
      }
    })

    val nodeindex = neo.index.getNodeAutoIndexer.getAutoIndex

    var latestknownhash = params.genesisBlock.getHash
    var toDo = List[Sha256Hash]()

    @tailrec def invertedToDoListOfHashes(block: StoredBlock, list: List[Sha256Hash]): List[Sha256Hash] = {
      if (block == null) return list // catch null-case from BitcoinJ
      val hash = block.getHeader.getHash
      if (hash == latestknownhash) list
      else invertedToDoListOfHashes(block.getPrev(blockStore), hash :: list)
    }

    def update() {
      // updates the Graph to reflect the known blockchain

      toDo = invertedToDoListOfHashes(chain.getChainHead, List())

      for (hash <- toDo) {

        while (peers.downloadPeer == null) // this is very ugly
          Thread.sleep(1000)
        for (trans: Transaction <- peers.downloadPeer.getBlock(hash).get.getTransactions)
        {
          // waits for every block!
          val hashString = trans.getHashAsString
          if (nodeindex.get("TransactionHash", hashString).getSingle == null)
          {
            println("Transaction " + hashString + " already exists in neodb")
            return
          }
          execInNeo4j
          {
            neo => // transaction network
              val node = neo.createNode()
              node("TransactionHash") = hashString
              if (!trans.isCoinBase) // record parent transactions if there is such a thing
                for (input <- trans.getInputs)
                  nodeindex.get("TransactionHash", input.getParentTransaction.getHashAsString).getSingle --> "getSpentBy" --> node
          }
        } //parallelize/unblock? watch out for transaction dependencies!
        // what if peer doesn't answer etc.? catch exceptions!
      }


    }

  }

  BlockChainDownloader()
  while (true)
  {
    Graph.update()
    println("update ready:" + Graph.toDo)
  }
}