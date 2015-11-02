package net.bitcoinprivacy.bge.models


import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{ StaticQuery => Q }
import scala.slick.jdbc.meta.MTable
import util.Hash
import org.bitcoinj.params.MainNetParams                                                                                                              
import org.bitcoinj.core.{Address => Add}
import org.bitcoinj.core.AddressFormatException                                                                                                              
import core._

// summary case class is the block model class
case class Transaction(hash: String, value: Long)

object Transaction extends core.BitcoinDB
{
  def get(blockHeight: Int, from: Int, until: Int) =
    transactionDBSession{

      val txList = movements.filter(_.height_in === blockHeight).groupBy(_.transaction_hash).map{ case (tx,rest) => (tx, rest.map(_.value).sum) }.drop(from).take(until-from) 
      txList.run.toVector.map{p => Transaction(Hash(p._1).toString, p._2.getOrElse(0L) )}

    }
}



