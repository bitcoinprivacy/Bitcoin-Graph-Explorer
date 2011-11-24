package BitcoinGraphExplorer

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 11/24/11
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */

import java.util.HashMap
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.{Config, EmbeddedGraphDatabase}
import org.neo4j.scala._
import collection.JavaConversions._

import com.google.bitcoin.core._

// beware: Transaction might be shadowed by neo4j

object Graph extends Neo4jWrapper {


  val config = new HashMap[String, String]; // turn on auto-indexing
  config.put(Config.NODE_KEYS_INDEXABLE, "TransactionHash,BlockHash");
  //config.put( Config.RELATIONSHIP_KEYS_INDEXABLE, "HasOutputs" );
  config.put(Config.NODE_AUTO_INDEXING, "true");
  config.put(Config.RELATIONSHIP_AUTO_INDEXING, "true");

  implicit val neo: GraphDatabaseService = new EmbeddedGraphDatabase("neodb", config)

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() {
      neo.shutdown
    }
  })

  val nodeindex = neo.index.getNodeAutoIndexer.getAutoIndex

  class NeoDownLoadListener(params: NetworkParameters) extends DownloadListener {

    override def onBlocksDownloaded(peer: Peer, block: Block, blocksLeft: Int) {

      execInNeo4j {
        neo => // this is very far out so as to keep the whole operation atomic

        // block network
          val blockHashString = block.getHashAsString
          if (nodeindex.get("BlockHash", blockHashString).getSingle != null)
            println("Block " + blockHashString + " already exists in neodb")
          val blocknode = neo.createNode()
          blocknode("BlockHash") = blockHashString

          val prevHashString = block.getPrevBlockHash.toString
          val prevnode = nodeindex.get("BlockHash", prevHashString).getSingle
          if (prevnode != null) blocknode --> "isPrecededBy" --> prevnode
          else if (prevHashString != params.genesisBlock.getHashAsString)
            throw new Exception("non-genesis Block without predecessor: "+ blockHashString)
          // todo: make exception handling nicer
            else {  // handle special case: implicit genesisBlock
              val genesisnode = neo.createNode()
              genesisnode("BlockHash") = prevHashString
              blocknode --> "isPrecededBy" --> genesisnode
            }

          // transaction network
          for (trans: Transaction <- block.getTransactions) {
            val transHashString = trans.getHashAsString
            if (nodeindex.get("TransactionHash", transHashString).getSingle != null)
              println("Transaction " + transHashString + " already exists in neodb")
            val node = neo.createNode()
            node("TransactionHash") = transHashString
            node --> "isRecordedIn" --> blocknode
            if (!trans.isCoinBase) // record parent transactions if there is such a thing
              for (input <- trans.getInputs)
                nodeindex.get("TransactionHash", input.getParentTransaction.getHashAsString).getSingle --> "getSpentBy" --> node
          }
      }

      super.onBlocksDownloaded(peer: Peer, block: Block, blocksLeft: Int) // to keep the nice statistics
    }
  }

}