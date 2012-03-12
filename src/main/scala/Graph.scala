package BitcoinGraphExplorer

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 11/24/11
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */

import java.util.HashMap
import org.neo4j.kernel.{Config, EmbeddedGraphDatabase}
import org.neo4j.scala._
import collection.JavaConversions._

import com.google.bitcoin.core._
import org.neo4j.graphdb.index.IndexManager
import org.neo4j.graphdb.{Direction, DynamicRelationshipType, GraphDatabaseService}

// beware: Transaction might be shadowed by neo4j

object Graph extends Neo4jWrapper {


  implicit val neo: GraphDatabaseService = new EmbeddedGraphDatabase("neodb")

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() {
      neo.shutdown
    }
  })


  val index = neo.index()
  val entityIndex = index.forNodes("entities")
  // val transactionIndex = index.forRelationships("transactions");


  class NeoDownLoadListener(params: NetworkParameters) extends DownloadListener {

    override def onBlocksDownloaded(peer: Peer, block: Block, blocksLeft: Int) {

      execInNeo4j {
        neo => // this is very far out so as to keep the whole operation atomic

        // the all new "controlling entities" network
          for (trans: Transaction <- block.getTransactions) {
            
            val node = if (!trans.isCoinBase) {

              // record incoming addresses if there is such a thing
              // all incoming addresses are controlled by a common entity/node
              // invariant kept: every address occurs in at most one node!

              var addresses: List[Address] = trans.getInputs.map(_.getFromAddress).toList
              val (unknown, known) = addresses.partition(entityIndex.get("address", _).getSingle == null)

              // get a List of all known unique entities that use addresses in this transaction input
              if (known.isEmpty) {
                val node = neo.createNode()
                for (address <- addresses)
                  entityIndex.add(node, "address", address)
                node("addresses") = addresses.toArray
                node
              }
              else {
                addresses = unknown
                val entities = known.map(entityIndex.get("address", _).getSingle).distinct
                val node = entities.head
                for (oldnode <- entities.tail) {
                  for (oldrel <- oldnode.getRelationships(Direction.OUTGOING))
                    node --> "pays" --> oldrel.getEndNode
                  for (oldrel <- oldnode.getRelationships(Direction.INCOMING))
                    node <-- "pays" <-- oldrel.getEndNode

                  addresses ++ oldnode("addresses")
                  entityIndex.remove(oldnode)
                  oldnode.delete()
                }

                for (address <- addresses)
                  entityIndex.add(node, "address", address)
                node("addresses") = node("addresses") ++ addresses
                node 
              }
            }
            else createIfNotPresent("address","0")
            
            for (output <- trans.getOutputs)
              try { 
              node.createRelationshipTo(createIfNotPresent("address",output.getScriptPubKey.getToAddress),"pays")("amount") = output.getValue
              }
              catch {
                case e: ScriptException => println("can't parse script paying from " + node("addresses"))
              }
          }


      }

      super.onBlocksDownloaded(peer: Peer, block: Block, blocksLeft: Int) // to keep the nice statistics
    }

    def createIfNotPresent(key: String, value: Any) = {
      var node = entityIndex.get(key, value).getSingle
      if (node != null)
        println(key + " " + value + " already exists in neodb")
      else {
        node = neo.createNode()
        entityIndex.add(node, key, value)
      }
      node
    }


  }

}