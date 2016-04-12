package net.bitcoinprivacy.bge.models

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

case class Distribution(addresses: Int, satoshis: Long)

object Distribution extends db.BitcoinDB
{
  def get(limit: Long) =   
    transactionDBSession{
      val a = balances.filter(_.balance > limit).map(_.balance)

      val result = a.groupBy(_ => true) // this is a workaround for a slick compiler inefficiency
        .map { case (_, as) =>
          (as.map(identity).size,as.map(identity).sum) }.firstOption.getOrElse(0,None)

      Distribution(result._1,result._2.getOrElse(0L))
    }
 }



