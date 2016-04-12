package net.bitcoinprivacy.bge.models

import core._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util.Hash

case class Transaction(hash: String, value: Long)
case class TransactionsSummary(value: Long, tx: Int, tstamp: Long)

object Transaction extends db.BitcoinDB {

  def get(blockHeight: Int, from: Int, until: Int) = transactionDBSession{

    val movementsList = movements.filter(_.height_in === blockHeight).groupBy(_.transaction_hash).map{ case (tx,rest) => (tx, rest.map(_.value).sum) }.run

    val utxosList = utxo.filter(_.block_height === blockHeight).groupBy(_.transaction_hash).map{ case (tx,rest) => (tx, rest.map(_.value).sum) }.run

    (movementsList ++ utxosList).groupBy(_._1).
      map {case (tx,list) => Transaction(Hash(tx).toString, list.map(_._2.getOrElse(0L)).sum)}
  }

  def  getInfo(blockHeight: Int) = transactionDBSession {

    val result = blockDB.filter(_.block_height===blockHeight).map(p => (p.btcs, p.txs, p.tstamp)).firstOption.getOrElse(0L,0,0L)

    (TransactionsSummary.apply _).tupled(result)

  }

}



