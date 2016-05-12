package core

import org.bitcoinj.core._
import util._

/**
 * Created by yzark on 25.08.14.
 */
trait BlockSource extends db.BitcoinDB {
  
  def blockSource: Iterator[(Block,Int)] // block,height

  def getCurrentLongestChainFromBlockCount = {

    val lastBlock = chain.getChainHead
    val lastNo = lastBlock.getHeight

    val blockList = (blockCount to lastNo).foldRight((Vector[(Sha256Hash,Int)](),lastBlock)){
      case (no,(vec,bl)) =>
        val previousElement = ((bl.getHeader.getHash,no)+:vec,bl.getPrev(blockStore))
        assert(bl.getHeader.getHash != bl.getPrev(blockStore).getHeader.getHash,
               bl.getHeader.getHash.toString +" != "+ bl.getPrev(blockStore).getHeader.getHash.toString + ", previous block is the same as current block!")
        previousElement
    }._1


    val hashes = blockList map (_._1)
    assert(hashes.length == hashes.distinct.length, "duplicate block hashes in blockChain (maybe we use bitcoinJ wrongly)")

    blockList
    
  }
}
