package core

import org.bitcoinj.core.{Block, NetworkParameters}
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BlockFileLoader
import org.bitcoinj.core._
import org.bitcoinj.store._
import java.net.InetAddress
import util._
import scala.collection.convert.WrapAsScala._
import sys.process._

/**
 * Created by yzark on 25.08.14.
 */
trait BlockSource {
  
  def blockSource: Iterator[(Block,Int)] // block,height


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
