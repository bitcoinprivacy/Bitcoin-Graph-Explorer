package BitcoinGraphExplorer

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 11/24/11
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */

import org.neo4j.kernel.EmbeddedGraphDatabase
import org.neo4j.scala._
import collection.JavaConversions._

import com.google.bitcoin.core._
import org.neo4j.graphdb.{Direction, GraphDatabaseService}
import scala.actors.Actor._
import actors.Actor
import collection.mutable.HashMap

// beware: Transaction might be shadowed by neo4j

object Graph extends Neo4jWrapper with EmbeddedGraphDatabaseServiceProvider {
  def neo4jStoreDir = "/tmp/temp-neo-test"

  implicit val neo: GraphDatabaseService = new EmbeddedGraphDatabase("neodb")

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() {
      neo.shutdown
    }
  })


  val index = neo.index()
  val entityIndex = index.forNodes("entities")
  val transactionIndex = index.forRelationships("transactions");


  val origin = withTx {
    implicit neo => createNodeWithAddressIfNotPresent("0")
  }

  val addressMap: HashMap[String,DisjointSetOfAddresses] = new HashMap[String, DisjointSetOfAddresses]()


  class NeoDownLoadListener(params: NetworkParameters) extends DownloadListener {

    disjointSetActor.start()

    object disjointSetActor extends Actor {
      def act {
        react {
          case block:Block =>
            // the all new "controlling entities" network
            val transactions = block.getTransactions
            for (trans: Transaction <- transactions) {

              if (!trans.isCoinBase) {

                // record incoming addresses if there is such a thing
                // all incoming addresses are controlled by a common entity/node
                // invariant kept: every address occurs in at most one node!

                val addresses = trans.getInputs.map(_.getFromAddress.toString)
                val disjointSets = addresses.map(x => addressMap.getOrElseUpdate(x,new DisjointSetOfAddresses(x)))
                disjointSets.reduceLeft((x,y) =>  x.union(y))
              }

              for (output <- trans.getOutputs)
              { val outaddress = 
                try {
                   output.getScriptPubKey.getToAddress(params).toString
                }
                catch {
                  case e: ScriptException =>
                    val script = output.getScriptPubKey.toString
                    if (script.startsWith("[65]")) {
                      val pubkeystring = script.substring(4, 134)
                      import Utils._
                      val pubkey = hex2Bytes(pubkeystring)
                      val address = new Address(params, sha256hash160(pubkey))
                      address.toString
                    }
                    // special case because bitcoinJ doesn't support pay-to-IP scripts
                    else { 
                      println("can't parse script: " + output.getScriptPubKey.toString)
                      "0"
                    }
                }
                addressMap.getOrElseUpdate(outaddress, new DisjointSetOfAddresses(outaddress))
            }


        }
        println(addressMap.size + " addresses")
        act()


        }
      }  
    }
    
    
    object neoActor extends Actor {
      def act {
        react {
          case block:Block =>
            withTx {
             implicit neo => // this is very far out so as to keep the whole operation atomic

              // the all new "controlling entities" network
                val transactions = block.getTransactions
                if (transactionIndex.get("transaction", transactions.head.getHashAsString).getSingle != null)
                  println("graphdb already has block" + block.getHashAsString)

                else for (trans: Transaction <- transactions) {


                  val transHash = trans.getHashAsString
                  val node = if (!trans.isCoinBase) {

                    // record incoming addresses if there is such a thing
                    // all incoming addresses are controlled by a common entity/node
                    // invariant kept: every address occurs in at most one node!

                    var addresses: Array[String] = trans.getInputs.map(_.getFromAddress.toString).toArray
                    val (unknown, known) = addresses.partition(entityIndex.get("address", _).getSingle == null)

                    // get a List (Arry) of all known unique entities that use addresses in this transaction input

                    if (known.isEmpty) {
                      val node = createNode
                      for (address <- addresses)
                        entityIndex.add(node, "address", address)
                      node("addresses") = addresses
                      node
                    }
                    else {
                      addresses = unknown
                      val entities = known.map(entityIndex.get("address", _).getSingle).distinct
                      val node = entities.head
                      for (oldnode <- entities.tail) {
                        for (oldrel <- oldnode.getRelationships(Direction.OUTGOING)) {
                          val rel = node.createRelationshipTo (oldrel.getEndNode,"pays")
                          rel("amount")= oldrel("amount") .get
                          val oldtrans = oldrel("transaction").get
                          rel("transaction") = oldtrans
                          transactionIndex.remove(oldrel)
                          transactionIndex.add(rel,"transaction",oldtrans)
                          oldrel.delete()
                        }
                        for (oldrel <- oldnode.getRelationships(Direction.INCOMING)) {
                          val rel = oldrel.getStartNode.createRelationshipTo(node,"pays")
                          rel("amount")= oldrel("amount").get
                          val oldtrans = oldrel("transaction").get
                          rel("transaction") = oldtrans
                          transactionIndex.remove(oldrel)
                          transactionIndex.add(rel,"transaction",oldtrans)
                          oldrel.delete()
                        }

                        addresses ++ oldnode("addresses") // is always distinct due to invariant
                        entityIndex.remove(oldnode)
                        oldnode.delete()
                      }

                      for (address <- addresses)
                        entityIndex.add(node, "address", address)
                      addresses ++ node("addresses")   // not sure why this compiles and if semantics is right
                      node("addresses") = addresses
                      node
                    }
                  }
                  else origin

                  for (output <- trans.getOutputs)
                    try {
                      val rel = node.createRelationshipTo(
                        createNodeWithAddressIfNotPresent(output.getScriptPubKey.getToAddress(params).toString), "pays")
                      rel("amount") = output.getValue.toString
                      rel("transaction") = transHash
                      transactionIndex.add(rel,"transaction", transHash)
                    }
                    catch {
                      case e: ScriptException =>
                        val script = output.getScriptPubKey.toString
                        if (script.startsWith("[65]")) {
                          val pubkeystring = script.substring(4, 134)
                          import Utils._
                          val pubkey = hex2Bytes(pubkeystring)
                          val address = new Address(params, sha256hash160(pubkey))
                          val rel = node.createRelationshipTo(createNodeWithAddressIfNotPresent(address.toString), "pays")
                          rel ("amount") = output.getValue.toString
                          rel ("transaction") = transHash
                          transactionIndex.add(rel,"transaction", transHash)
                        }
                        // special case because bitcoinJ doesn't support pay-to-IP scripts
                        else println("can't parse script: " + output.getScriptPubKey.toString)
                    }
                }


            }
            act()

        }
      }
    }

    override def onBlocksDownloaded(peer: Peer, block: Block, blocksLeft: Int) {

      disjointSetActor ! block

      super.onBlocksDownloaded(peer: Peer, block: Block, blocksLeft: Int) // to keep the nice statistics
    }


    def hex2Bytes(hex: String): Array[Byte] = {
      (for {i <- 0 to hex.length - 1 by 2 if i > 0 || !hex.startsWith("0x")}
      yield hex.substring(i, i + 2))
        .map(Integer.parseInt(_, 16).toByte).toArray
    }

  }


  def createNodeWithAddressIfNotPresent(value: String) = {
    var node = entityIndex.get("address", value).getSingle
    if (node != null)
      println(value + " already exists in neodb")
    else {
      node = neo.createNode
      node("addresses") = Array(value)
      entityIndex.add(node, "address", value)
    }
    node
  }

}