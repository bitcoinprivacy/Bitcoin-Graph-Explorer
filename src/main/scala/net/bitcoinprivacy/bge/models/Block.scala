package net.bitcoinprivacy.bge.models


import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{ StaticQuery => Q }
import scala.slick.jdbc.meta.MTable
import util.Hash

case class Block(hash: String, height: Int, tx: Int, value:Long, tstamp: Long)
case class BlockSummary(count: Long, txCount: Long)

object Block extends core.BitcoinDB
{
  def getBlocks(from: Int, until: Int) =

    transactionDBSession{

      val max = stats.map(_.block_height).max.run.getOrElse(0)
      val blockslist = for (b<- blockDB.filter(_.block_height < max).sortBy(_.block_height asc).drop(from).take(until-from))
                       yield (b.hash, b.block_height, b.txs,b.btcs, b.tstamp)

      blockslist.run.toVector map (p => Block(Hash(p._1).toString, p._2,p._3,p._4,p._5))

    }

  def getSummary = {
    transactionDBSession{
      val summ = stats.sortBy(_.block_height.desc).map{o=>(o.block_height, o.total_transactions)}.first
      BlockSummary(summ._1, summ._2)
      
      
    }
  }
}



