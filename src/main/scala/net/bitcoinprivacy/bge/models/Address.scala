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

case class Address(address: String, balance: Long)
case class AddressesSummary(count: Int, sum: Long)

object Address extends core.BitcoinDB {

  def getWallet(hash: Array[Byte], from: Int, until: Int) = transactionDBSession {

    val repOpt = addresses.filter(_.hash === hash).map(_.representant).firstOption

    repOpt match {
      case None =>
        List(Address(hashToAddress(hash), balances.filter(_.address === hash).map(_.balance).firstOption.getOrElse(0L)))
      case Some(rep) =>

        val query = for {
          a <- addresses
          if a.representant === rep
        }  yield (a.hash)

        val walletVector = query.
          drop(from).take(until-from).run

        val bQuery = balances.filter(_.address inSet walletVector).map(p=> (p.address,p.balance))
        bQuery.run.map{p => Address(hashToAddress(p._1), p._2 )}
    }
  }

  def getWalletSummary(hash: Array[Byte]) = transactionDBSession {
    
    val repOpt = addresses.filter(_.hash === hash).map(_.representant).firstOption

    repOpt match {
      case None =>
        AddressesSummary(1, balances.filter(_.address === hash).map(_.balance).firstOption.getOrElse(0L))
      case Some(rep) =>

        val query = for {
          a <- addresses
          if a.representant === rep
        }  yield (a.hash)

        val result = (query.size.run, closureBalances.filter(_.address === rep).map(_.balance).firstOption.getOrElse(0L))

        AddressesSummary(result._1, result._2)
      
    }
  }

  def getAddressList[A <: Table[_] with BalanceField with HashField with BlockHeightField](richListTable: TableQuery[A], from: Int, until: Int): List[Address] = transactionDBSession {

    richListTable.sortBy(p => (p.block_height.desc,p.balance.desc)).
      drop(from).take(Math.min(1000, until-from)).map(p=> (p.hash,p.balance)).run.toList map
        (p => Address(hashToAddress(p._1), p._2))

  }

  

  def getAddressListSummary[A <: Table[_] with BalanceField with HashField with BlockHeightField](richListTable: TableQuery[A]): AddressesSummary = transactionDBSession {

    val query = richListTable.sortBy(p => (p.block_height.desc,p.balance.desc))
      .take(1000).map(_.balance)

    val result = query.groupBy(_ => true)
      .map { case (_, as) => (as.map(identity).size,as.map(identity).sum) }.firstOption.getOrElse(0,None)
    
    AddressesSummary(
      result._1,result._2.getOrElse(0L)
    )

  }

  def hashToAddress(hash: Array[Byte]): String = hash.length match {

    case 20 => new Add(MainNetParams.get,0,hash).toString

    case 21 => new Add(MainNetParams.get,hash.head.toInt,hash.tail).toString

    case 0 => "No decodable address found"

    case x if (x%20==1) =>

      (for (i <- 1 to hash.length-20 by 20)
       yield hashToAddress(hash.slice(i,i+20)) ).mkString(",")

    case _  => hash.length + " undefined"
  }

}
