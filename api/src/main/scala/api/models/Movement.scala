package net.bitcoinprivacy.bge.models

import scala.slick.driver.PostgresDriver.simple._

import util.Hash

case class Movement(tx: String, value:Long, spentInTx: String, address: String)

case class MovementsSummary(sum: Long, count: Long, minHeight: Int, maxHeight: Int)

object Movement extends db.BitcoinDB {
  def getMovements(address: Array[Byte], from: Int, until: Int) =
    get(_.address===address, from, until)

  def getInputs(transactionHash: Array[Byte], from: Int, until: Int) =
    get(_.spent_in_transaction_hash === transactionHash, from, until)

  def getOutputs(transactionHash: Array[Byte], from: Int, until: Int) =
    get(_.transaction_hash===transactionHash,from,until)
  
  def getMovementsSummary(address: Array[Byte]) = getSummary(_.address===address)
  def getOutputsSummary(transactionHash: Array[Byte]) = getSummary(_.transaction_hash===transactionHash)
  def getInputsSummary(transactionHash: Array[Byte]) = getSummary(_.spent_in_transaction_hash===transactionHash)

  private def get[T <: Column[_]](f: db.Movements => T, from: Int, until: Int)(implicit wt: scala.slick.lifted.CanBeQueryCondition[T]) =
    DB withSession { implicit session =>

      val inputs = for (b<-movements filter f drop from take (until-from))
                   yield (b.transaction_hash, b.value, b.spent_in_transaction_hash, b.address)

      inputs.run map (p => Movement(Hash(p._1).toString, p._2, Hash(p._3).toString, Address.hashToAddress(p._4)))

    }

  private def getSummary[T <: Column[_]](f: db.Movements => T)(implicit wt: scala.slick.lifted.CanBeQueryCondition[T]) =
    DB withSession { implicit session =>
      val query = movements.filter(f).map( p => (p.value, p.height_out))

      val result = query.groupBy(_ => true) // this is a workaround for a slick compiler inefficiency
        .map { case (_, as) =>
          (as.map(_._1).size,as.map(_._1).sum,as.map(_._2).min, as.map(_._2).max) }.firstOption.getOrElse(0,None, None, None)

      MovementsSummary(
        result._2.getOrElse(0L), result._1 ,result._3.getOrElse(Int.MaxValue),result._4.getOrElse(0)
      )
    }

}
