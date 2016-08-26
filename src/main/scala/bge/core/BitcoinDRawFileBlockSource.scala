package core

import org.bitcoinj.core._
import util._

import scala.collection.convert.WrapAsScala._

// In java that should be implements libs.BlockSource
trait BitcoinDRawFileBlockSource extends BlockSource
{
  override def blockSource: Iterator[(Block,Int)] = {

    startBitcoinJ
    log.info("starting")
    val almostCurrentChain = getCurrentLongestChainFromBlockCount
    val blockMap = almostCurrentChain toMap

    for {
      block <- asScalaIterator(loader)
      hash = block.getHash
      if blockMap.contains(hash)      
    }
    yield 
      (block, blockMap(hash))
  }
  
}
