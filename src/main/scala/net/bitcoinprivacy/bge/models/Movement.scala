package net.bitcoinprivacy.bge.models


import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{ StaticQuery => Q }
import scala.slick.jdbc.meta.MTable
import util.Hash

case class Movement(tx: String, value:Long, spentInTx: String, address: String)

case class MovementsSummary(sum: Long, count: Long, maxHeight: Int, minHeight: Int)

object Movement extends core.BitcoinDB
{
  def getMovements(address: Array[Byte], from: Int, until: Int) = transactionDBSession {

    val add = Address.hashToAddress(address)

    val mvmnts = for (b<- movements.filter(_.address===address).drop(from).take(until-from))
                 yield (b.transaction_hash ,b.value, b.spent_in_transaction_hash)

    mvmnts.run.toVector map (p => Movement(Hash(p._1).toString, p._2, Hash(p._3).toString, add))

  }

  def getInputs(transactionHash: Array[Byte], from: Int, until: Int) = transactionDBSession {

    val inputs = for (b<-movements.filter(_.spent_in_transaction_hash === transactionHash).drop(from).take(until-from))
                 yield (b.transaction_hash, b.value, b.spent_in_transaction_hash, b.address)

    inputs.run.toVector map (p => Movement(Hash(p._1).toString, p._2, Hash(p._3).toString, Address.hashToAddress(p._4)))

  }

  def getOutputs(transactionHash: Array[Byte], from: Int, until: Int) = transactionDBSession {

    val outputs = for(b<-movements.filter(_.transaction_hash===transactionHash).drop(from).take(until-from))
                  yield (b.transaction_hash, b.value, b.spent_in_transaction_hash, b.address )

    outputs.run.toVector map (p => Movement(Hash(p._1).toString, p._2, Hash(p._3).toString, Address.hashToAddress(p._4)))

  }

  def getMovementsSummary(address: Array[Byte]) = transactionDBSession {
    MovementsSummary(movements.filter(_.address===address).size.run, 1, 2, 3)
  }

  def getOutputsSummary(transactionHash: Array[Byte]) = transactionDBSession {
    MovementsSummary(movements.filter(_.transaction_hash===transactionHash).size.run, 1, 2, 3)
  }

  def getInputsSummary(transactionHash: Array[Byte]) = transactionDBSession {
    MovementsSummary(movements.filter(_.spent_in_transaction_hash===transactionHash).size.run, 1, 2, 3)
  }

}



