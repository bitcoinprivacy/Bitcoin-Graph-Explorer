package net.bitcoinprivacy.bge.models


import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{ StaticQuery => Q }
import scala.slick.jdbc.meta.MTable
import util.Hash

case class AdvancedStats(block_height:Int, total_bitcoins_in_addresses:Int, total_transactions:Int, total_addresses:Int, total_closures:Int, total_addresses_with_balance:Int, total_closures_with_balance:Int, total_addresses_no_dust:Int, total_closures_no_dust:Int, gini_closure:Double,gini_address:Double, tstamp:Long, average_read_blocks: Int, average_read_transactions: Int, average_time: Long, iterations: Int, last_read_blocks: Int, last_read_transactions: Int, last_read_time: Long)

case class Stats(block_height:Int, total_bitcoins_in_addresses:Int, total_transactions:Int, total_addresses:Int, total_closures:Int, total_addresses_with_balance:Int, total_closures_with_balance:Int, total_addresses_no_dust:Int, total_closures_no_dust:Int, gini_closure:Double,gini_address:Double, tstamp:Long)

object Stats extends core.BitcoinDB
{
  def getAdvancedStats =
    transactionDBSession{

      val p1 = stats.sortBy(_.block_height.desc).first
      val p2 = stats.sortBy(_.block_height.desc).drop(1).take(1).run.toList.head
      val pL = stats.sortBy(_.block_height.asc).first
      val c = stats.length.run

      /*
       count(*),
       max(block_height) / count(*) - min(block_height) / count(*),
       (max(total_transactions)-min(total_transactions)) / (max(block_height)-min(block_height)) from stats;
       (max(tstamp) - min(tstamp)) / (60 * count(*),
       */

      AdvancedStats(
        p1._1,
        p1._2,
        p1._3,
        p1._4, // total_addresses
        p1._5, // total_closures
        p1._6,
        p1._7,
        p1._8,
        p1._9,
        p1._10,
        p1._11,
        p1._12,
        (p1._1-pL._1)/c, // avgBlocks
        (p1._3-pL._3)/(p1._1-pL._1), // avgTxs
        (p1._12-pL._12)/c,// avgTime
        c,
        (p1._1-p2._1), // LastBlocks
        (p1._3-p2._3)/(p1._1-p2._1), // LastTxs
        p1._12-p2._12// LastTime
      )

    }


  def getStats() =
    transactionDBSession{

      val p1 = stats.sortBy(_.block_height.desc).run.toList

      p1 map {
        p => Stats(p._1,p._2,p._3,p._4,p._5,p._6,p._7,p._8,p._9,p._10,p._11,p._12)
      }
    }
}



