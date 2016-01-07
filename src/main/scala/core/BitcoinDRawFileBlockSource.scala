package core

import org.bitcoinj.core._
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BlockFileLoader
import org.bitcoinj.store.MemoryBlockStore;
import java.net.InetAddress


import scala.collection.convert.WrapAsScala._

// In java that should be implements libs.BlockSource
trait BitcoinDRawFileBlockSource extends BlockSource
{
  override def blockSource: Iterator[(Block,Int)] = {
    start
    
    println("starting at " +  java.util.Calendar.getInstance().getTime())
    val b = for {
      block <- asScalaIterator(loader)
      storedBlock = blockStore.get(block.getHash)  
      if storedBlock != null
    }
    yield 
      (block,storedBlock.getHeight)

    stop
    
    b
  }
  
}
