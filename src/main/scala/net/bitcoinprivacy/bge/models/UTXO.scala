package net.bitcoinprivacy.bge.models


import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{ StaticQuery => Q }
import scala.slick.jdbc.meta.MTable
import util.Hash

case class UTXO(tx: String, value:Long, address: String)
case class UTXOsSummary(count : Int, sum: Long)

object UTXO extends core.BitcoinDB
{
  def getUtxosByAd(address: Array[Byte], from: Int, until: Int) =
    transactionDBSession{

      val ad = Address.hashToAddress(address)
      val utxos = for (b<- utxo.filter(_.address===address).drop(from).take(until-from)) 
                       yield (b.transaction_hash ,b.value)

      utxos.run.toVector map (p => UTXO(Hash(p._1).toString, p._2, ad))

    }

  def getUtxosByTx(transactionHash: Array[Byte], from: Int, until: Int) =
    transactionDBSession {

      val tx = Hash(transactionHash).toString
      val outputsFromUTXOS = for (b<-utxo.filter(_.transaction_hash===transactionHash).drop(from).take(until-from))
                             yield (b.address, b.value)

      outputsFromUTXOS.run.toVector map (p => UTXO(tx, p._2, Address.hashToAddress(p._1)))
    }

  def getUtxosByAdSummary(address: Array[Byte]) =
    transactionDBSession{
      val query = utxo.filter(_.address===address).map(_.value)
      UTXOsSummary(query.size.run, query.sum.run.getOrElse(0L))
    }

  def getUtxosByTxSummary(transactionHash: Array[Byte]) =
    transactionDBSession {
      val query = utxo.filter(_.transaction_hash===transactionHash).map(_.value)
      UTXOsSummary(query.size.run, query.sum.run.getOrElse(0L))
    }
}
