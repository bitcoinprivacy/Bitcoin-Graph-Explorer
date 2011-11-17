package com.google.bitcoin.core

import com.google.bitcoin.discovery.{PeerDiscoveryException, PeerDiscovery}
import com.google.bitcoin.store.{BlockStoreException, BlockStore}
import java.io.IOException
import java.net.{InetSocketAddress, ConnectException, SocketTimeoutException}
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger
import java.util.{HashSet, Collections,Set}
import org.slf4j.{LoggerFactory, Logger}


/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 10/19/11
 * Time: 5:10 PM
 * To change this template use File | Settings | File Templates.
 pimp my library-style extension of PeerGroup functionality in order to (re-) use the private methods in PeerGroup

 */
object PublicPeerGroup {
  val DEFAULT_CONNECTION_DELAY_MILLIS: Int = 5 * 1000
  val CORE_THREADS: Int = 1
  val THREAD_KEEP_ALIVE_SECONDS: Int = 1
  val DEFAULT_CONNECTIONS: Int = 4
  val log: Logger = LoggerFactory.getLogger(classOf[PeerGroup])
}

class PublicPeerGroup {
  import PublicPeerGroup._
  private var inactives: BlockingQueue[PeerAddress] = null
  private var connectThread: Thread = null
  private var running: Boolean = false
  private var peerPool: ThreadPoolExecutor = null
  private var peers: Set[Peer] = null
  var downloadPeer: Peer = null
  private var downloadListener: PeerEventListener = null
  private var peerEventListeners: Set[PeerEventListener] = null
  private var peerDiscoverers: Set[PeerDiscovery] = null
  private var params: NetworkParameters = null
  private var blockStore: BlockStore = null
  private var chain: BlockChain = null
  private var connectionDelayMillis: Int = 0

  /**
   * Creates a PeerGroup with the given parameters. The connectionDelayMillis parameter controls how long the
   * PeerGroup will wait between attempts to connect to nodes or read from any added peer discovery sources.
   */

  def this(blockStore: BlockStore, params: NetworkParameters, chain: BlockChain, connectionDelayMillis: Int) {
    this()
    this.blockStore = blockStore
    this.params = params
    this.chain = chain
    this.connectionDelayMillis = connectionDelayMillis
    inactives = new LinkedBlockingQueue[PeerAddress]
    peers = Collections.synchronizedSet(new HashSet[Peer])
    peerEventListeners = Collections.synchronizedSet(new HashSet[PeerEventListener])
    peerDiscoverers = Collections.synchronizedSet(new HashSet[PeerDiscovery])
    import PublicPeerGroup._  // strange, why don't we know it?
    peerPool = new ThreadPoolExecutor(CORE_THREADS, DEFAULT_CONNECTIONS, THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue[Runnable](1), PeerGroupThreadFactory)
  }

  /**
     * Creates a PeerGroup with the given parameters and a default 5 second connection timeout.
     */
  def this(blockStore: BlockStore, params: NetworkParameters, chain: BlockChain) {

    this(blockStore, params, chain, PublicPeerGroup.DEFAULT_CONNECTION_DELAY_MILLIS)
    }

  /**
   * Callbacks to the listener are performed in the connection thread.  The callback
   * should not perform time consuming tasks.
   */
  def addEventListener(listener: PeerEventListener): Unit = {
    peerEventListeners.add(listener)
  }

  def removeEventListener(listener: PeerEventListener): Boolean = {
    return peerEventListeners.remove(listener)
  }

  /**
   * Depending on the environment, this should normally be between 1 and 10, default is 4.
   *
   * @param maxConnections the maximum number of peer connections that this group will try to make.
   */
  def setMaxConnections(maxConnections: Int): Unit = {
    peerPool.setMaximumPoolSize(maxConnections)
  }

  def getMaxConnections: Int = {
    return peerPool.getMaximumPoolSize
  }

  /**Add an address to the list of potential peers to connect to */
  def addAddress(peerAddress: PeerAddress): Unit = {
    inactives.add(peerAddress)
  }

  /**Add addresses from a discovery source to the list of potential peers to connect to */
  def addPeerDiscovery(peerDiscovery: PeerDiscovery): Unit = {
    peerDiscoverers.add(peerDiscovery)
  }

  /**Starts the background thread that makes connections. */
  def start(): Unit = {
    this.connectThread = new Thread(new PeerExecutionRunnable, "Peer group thread")
    running = true
    this.connectThread.start
  }

  /**
   * Stop this PeerGroup
   *
   * <p>The peer group will be asynchronously shut down.  After it is shut down
   * all peers will be disconnected and no threads will be running.
   */
  def stop: Unit = {
    if (running) {
      connectThread.interrupt
    }
  }

  /**
   * Broadcast a transaction to all connected peers
   *
   * @return whether we sent to at least one peer
   */
  def broadcastTransaction(tx: Transaction): Boolean = {
    var success: Boolean = false
    peers synchronized {
      import scala.collection.JavaConversions._
      for (peer <- peers) {
        try {
          peer.broadcastTransaction(tx)
          success = true
        }
        catch {
          case e: IOException => {
            log.error("failed to broadcast to " + peer, e)
          }
        }
      }
    }
    return success
  }

  private final class PeerExecutionRunnable extends Runnable {
    /**
     * Repeatedly get the next peer address from the inactive queue
     * and try to connect.
     *
     * <p>We can be terminated with Thread.interrupt.  When an interrupt is received,
     * we will ask the executor to shutdown and ask each peer to disconnect.  At that point
     * no threads or network connections will be active.
     */
    def run: Unit = {
      try {
        while (running) {
          if (inactives.size == 0) {
            discoverPeers
          }
          else {
            tryNextPeer
          }
          Thread.sleep(connectionDelayMillis)
        }
      }
      catch {
        case ex: InterruptedException => {
          this synchronized {
            running = false
          }
        }
      }
      peerPool.shutdownNow
      peers synchronized {
        import scala.collection.JavaConversions._
        for (peer <- peers) {
          peer.disconnect
        }
      }
    }

    private def discoverPeers: Unit = {
      import scala.collection.JavaConversions._
      for (peerDiscovery <- peerDiscoverers) {
        var addresses: Array[InetSocketAddress] = null
        try {
          addresses = peerDiscovery.getPeers
        }
        catch {
          case e: PeerDiscoveryException => {
            log.error("Failed to discover peer addresses from discovery source", e)
            return
          }
        }
        {
          var i: Int = 0
          while (i < addresses.length) {
            {
              inactives.add(new PeerAddress(addresses(i)))
            }
            ({
              i += 1; i
            })
          }
        }
        if (inactives.size > 0) return //break is not supported
      }
    }

    /**Try connecting to a peer.  If we exceed the number of connections, delay and try again. */
    private def tryNextPeer: Unit = {
      val address: PeerAddress = inactives.take
      while (true) {
        try {
          val peer: Peer = new Peer(params, address, blockStore.getChainHead.getHeight, chain)
          var command: Runnable = new Runnable {
            def run: Unit = {
              try {
                log.info("Connecting to " + peer)
                peer.connect
                peers.add(peer)
                handleNewPeer(peer)
                peer.run
              }
              catch {
                case ex: PeerException => {
                  val cause: Throwable = ex.getCause
                  if (cause.isInstanceOf[SocketTimeoutException]) {
                    log.info("Timeout talking to " + peer + ": " + cause.getMessage)
                  }
                  else if (cause.isInstanceOf[ConnectException]) {
                    log.info("Could not connect to " + peer + ": " + cause.getMessage)
                  }
                  else if (cause.isInstanceOf[IOException]) {
                    log.info("Error talking to " + peer + ": " + cause.getMessage)
                  }
                  else {
                    log.error("Unexpected exception whilst talking to " + peer, ex)
                  }
                }
              }
              finally {
                peer.disconnect
                inactives.add(address)
                if (peers.remove(peer)) handlePeerDeath(peer)
              }
            }
          }
          peerPool.execute(command)
          return // break is not supported
        }
        catch {
          case e: RejectedExecutionException => {
          }
          case e: BlockStoreException => {
            log.error("Block store corrupt?", e)
            running = false
            throw new RuntimeException(e)
          }
        }
        Thread.sleep(connectionDelayMillis)
      }
    }
  }

  /**
   * Start downloading the blockchain from the first available peer.
   *
   * <p>If no peers are currently connected, the download will be started
   * once a peer starts.  If the peer dies, the download will resume with another peer.
   *
   * @param listener a listener for chain download events, may not be null
   */
  def startBlockChainDownload(listener: PeerEventListener): Unit = {
    this.downloadListener = listener
    peers synchronized {
      if (!peers.isEmpty) {
        startBlockChainDownloadFromPeer(peers.iterator.next)
      }
    }
  }

  /**
   * Download the blockchain from peers.<p>
   *
   * This method waits until the download is complete.  "Complete" is defined as downloading
   * from at least one peer all the blocks that are in that peer's inventory.
   */
  def downloadBlockChain: Unit = {
    var listener: DownloadListener = new DownloadListener
    startBlockChainDownload(listener)
    try {
      listener.await
    }
    catch {
      case e: InterruptedException => {
        throw new RuntimeException(e)
      }
    }
  }

  protected def handleNewPeer(peer: Peer): Unit = {
    if (downloadListener != null && downloadPeer == null) startBlockChainDownloadFromPeer(peer)
    peerEventListeners synchronized {
      import scala.collection.JavaConversions._
      for (listener <- peerEventListeners) {
        listener synchronized {
          listener.onPeerConnected(peer, peers.size)
        }
      }
    }
  }

  protected def handlePeerDeath(peer: Peer): Unit = {
    if (peer eq downloadPeer) {
      downloadPeer = null
      peers synchronized {
        if (downloadListener != null && !peers.isEmpty) {
          startBlockChainDownloadFromPeer(peers.iterator.next)
        }
      }
    }
    peerEventListeners synchronized {
      import scala.collection.JavaConversions._
      for (listener <- peerEventListeners) {
        listener synchronized {
          listener.onPeerDisconnected(peer, peers.size)
        }
      }
    }
  }

  private def startBlockChainDownloadFromPeer(peer: Peer): Unit = {
    peer.addEventListener(downloadListener)
    try {
      peer.startBlockChainDownload
    }
    catch {
      case e: IOException => {
        log.error("failed to start block chain download from " + peer, e)
        return
      }
    }
    downloadPeer = peer
  }

   object PeerGroupThreadFactory extends ThreadFactory {
     val poolNumber: AtomicInteger = new AtomicInteger(1)
     val threadNumber: AtomicInteger = new AtomicInteger(1)
     val group = Thread.currentThread.getThreadGroup
     val namePrefix = "PeerGroup-" + poolNumber.getAndIncrement + "-thread-"


    def newThread(r: Runnable): Thread = {
      var t: Thread = new Thread(group, r, namePrefix + threadNumber.getAndIncrement, 0)
      t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.currentThread.getPriority - 1))
      t.setDaemon(true)
      return t
    }



  }
}