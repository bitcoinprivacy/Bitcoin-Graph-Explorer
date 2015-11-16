package net.bitcoinprivacy.bge.models


import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{ StaticQuery => Q }
import scala.slick.jdbc.meta.MTable
import util.Hash

case class Distribution(addresses: Int, satoshis: Long)

object Distribution extends core.BitcoinDB
{
  def get(limit: Long) =   
    transactionDBSession{
      val a = balances.filter(_.balance > limit).map(_.balance)
      Distribution(a.length.run, a.sum.run.getOrElse(0))
    }
 }



