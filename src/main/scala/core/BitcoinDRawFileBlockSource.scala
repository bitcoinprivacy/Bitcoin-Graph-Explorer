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
    for {
      block <- asScalaIterator(loader)
      storedBlock = blockStore.get(block.getHash)  
      if storedBlock != null
    }
    yield 
      (block,storedBlock.getHeight)
  }
  
}
