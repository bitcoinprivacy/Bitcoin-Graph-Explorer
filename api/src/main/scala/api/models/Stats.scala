package net.bitcoinprivacy.bge.models

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

case class Stats(block_height:Int, total_bitcoins_in_addresses:Long, total_transactions:Long, total_addresses:Long, total_closures:Long, total_addresses_with_balance:Long, total_closures_with_balance:Long, total_addresses_no_dust:Long, total_closures_no_dust:Long, gini_closure:Double,gini_address:Double, tstamp:Long)


object Stats extends db.BitcoinDB
{ 
  def getStats =
    transactionDBSession{
      val o = stats.sortBy(_.block_height.desc).firstOption

      for (p <- o)
      yield Stats(p._1-1,p._2,p._3,p._4,p._5,p._6,p._7,p._8,p._9,p._10,p._11,p._12)
    }

  def getAllStats =
    transactionDBSession{

      val p1 = stats.sortBy(_.block_height.desc).run.toList

      p1 map {
        p => Stats(p._1,p._2,p._3,p._4,p._5,p._6,p._7,p._8,p._9,p._10,p._11,p._12)
      }
    }
}



