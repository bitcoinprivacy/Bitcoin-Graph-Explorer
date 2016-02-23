package core

import org.bitcoinj.core._
import util._

import scala.collection.convert.WrapAsScala._

// In java that should be implements libs.BlockSource
trait BitcoinDRawFileBlockSource extends BlockSource
{
  override def blockSource: Iterator[(Block,Int)] = {

    startBitcoinJ
    println("starting at " +  java.util.Calendar.getInstance().getTime())
    val almostCurrentChain = getCurrentLongestChainFromBlockCount dropRight 5
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
