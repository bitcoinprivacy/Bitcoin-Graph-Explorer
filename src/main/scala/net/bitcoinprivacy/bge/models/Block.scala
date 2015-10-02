package net.bitcoinprivacy.bge.models


import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{ StaticQuery => Q }
import scala.slick.jdbc.meta.MTable
import util.Hash

case class Block(hash: String, height: Int, tx: Int, value:Long, tstamp: Long)

object Block extends core.BitcoinDB
{
  def getBlocks(page: Int) =
    transactionDBSession{

      val blockslist = for (b<- blockDB.drop((page-1)*1000).take(1000)) 
                       yield (b.hash, b.block_height, b.txs,b.btcs, b.tstamp)

      blockslist.run.toVector map (p => Block(Hash(p._1).toString, p._2,p._3,p._4,p._5))

    }
}



