package net.bitcoinprivacy.bge.models

import scala.slick.driver.PostgresDriver.simple._

import util.Hash

case class UTXO(tx: String, value:Long, address: String, blockHeight: Int)
case class UTXOsSummary(count : Int, sum: Long, minHeight: Int, maxHeight: Int)

object UTXO extends db.BitcoinDB{

  def getUtxosByAd(address: Array[Byte], from: Int, until: Int) = DB withSession { implicit session =>

    val ad = Address.hashToAddress(address)

    val utxos = for (b<- utxo.filter(_.address===address).drop(from).take(until-from))
                yield (b.transaction_hash ,b.value, b.block_height)

    utxos.run map (p => UTXO(Hash(p._1).toString, p._2, ad, p._3))

  }

  def getUtxosByTx(transactionHash: Array[Byte], from: Int, until: Int) = DB withSession { implicit session =>

    val tx = Hash(transactionHash).toString

    val outputsFromUTXOS = for (b<-utxo.filter(_.transaction_hash===transactionHash).drop(from).take(until-from))
                           yield (b.address, b.value, b.block_height)

    outputsFromUTXOS.run map (p => UTXO(tx, p._2, Address.hashToAddress(p._1),  p._3))

  }

  def getUtxosByAdSummary(address: Array[Byte]) = getSummary(_.address===address)
  def getUtxosByTxSummary(transactionHash: Array[Byte]) = getSummary(_.transaction_hash===transactionHash)

  private def getSummary[T <: Column[_]](f: db.UTXO => T)(implicit wt: scala.slick.lifted.CanBeQueryCondition[T]) =
    DB withSession { implicit session =>

      val query = utxo filter f map ( p => (p.value, p.block_height))

      val result = query.groupBy(_ => true)
        .map { case (_, as) =>
          (as.map(_._1).size,as.map(_._1).sum,as.map(_._2).min, as.map(_._2).max) }.firstOption.getOrElse(0,None, None, None)

      UTXOsSummary(
        result._1,result._2.getOrElse(0L),result._3.getOrElse(Int.MaxValue),result._4.getOrElse(0)
      )

    }


}
