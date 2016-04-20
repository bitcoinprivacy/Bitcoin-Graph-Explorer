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
      (blockCount to lastNo).foldRight((Vector[(Sha256Hash,Int)](),lastBlock)){
        case (no,(vec,bl)) => ((bl.getHeader.getHash,no)+:vec,bl.getPrev(blockStore))}._1
  }


  // @annotation.tailrec final def waitIfNewBlocks(last: Int): Unit = {

  //   val waitTime = 10000
  //   Thread sleep waitTime
  //   val current = chain.getBestChainHeight

  //   println("waiting " + waitTime + " for bitcoinJ: last state (0 is error): " + last + ". current state:"+ current + " at " +  java.util.Calendar.getInstance().getTime())

  //   if (current ==  0 || ( /*current < 10000 &&*/ last != current))
  //     waitIfNewBlocks(current)
  //   else
  //     println("done")
  // }


}
