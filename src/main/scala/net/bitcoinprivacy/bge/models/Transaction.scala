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
import scala.collection.immutable.HashMap

// summary case class is the block model class
case class Transaction(hash: String, value: Long)
case class TransactionsSummary(value: Long, tx: Int, tstamp: Long)

object Transaction extends core.BitcoinDB {

  def get(blockHeight: Int, from: Int, until: Int) = transactionDBSession{

    val movementsList = movements.filter(_.height_in === blockHeight).groupBy(_.transaction_hash).map{ case (tx,rest) => (tx, rest.map(_.value).sum) }.run.toVector

    val utxosList = utxo.filter(_.block_height === blockHeight).groupBy(_.transaction_hash).map{ case (tx,rest) => (tx, rest.map(_.value).sum) }.run.toVector

    val totalList = (movementsList ++ utxosList).groupBy(_._1).
      map {case (tx,list) => (  tx,list. map(_._2.getOrElse(0L)).sum)}.
      toVector

    totalList.map (p => Transaction(Hash(p._1).toString, p._2.toLong))

  }

  def  getInfo(blockHeight: Int) = transactionDBSession {

    val query = blockDB.filter(_.block_height===blockHeight)

    TransactionsSummary(
      query.map(_.btcs).firstOption.getOrElse(0L),
      query.map(_.txs).firstOption.getOrElse(0),
      query.map(_.tstamp).firstOption.getOrElse(0L)
    )

  }

}



