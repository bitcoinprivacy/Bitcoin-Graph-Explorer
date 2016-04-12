package net.bitcoinprivacy.bge.models

import db._
import org.bitcoinj.core.{Address => Add}
import org.bitcoinj.params.MainNetParams
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

case class Address(address: String, balance: Long)
case class AddressesSummary(count: Int, sum: Long)

object Address extends db.BitcoinDB {

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

        val wallet = query.
          drop(from).take(until-from).run

        val bQuery = balances.filter(_.address inSetBind wallet).map(p=> (p.address,p.balance))
        val balanced = bQuery.run map (p => (hashToAddress(p._1), p._2))
        val unbalanced = ((wallet map hashToAddress).toSet -- (balanced map (_._1))) map ((_,0L))
        (balanced ++ unbalanced).toVector.sortBy(-_._2).
          map{p => (Address.apply _).tupled(p)}
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

  def getAddressList[A <: Table[_] with BalanceField with HashField with BlockHeightField](richListTable: TableQuery[A], blockHeight: Int, from: Int, until: Int): List[Address] = transactionDBSession {

    richListTable.filter(_.block_height === blockHeight).sortBy(_.balance.desc).
      drop(from).take(Math.min(1000, until-from)).map(p=> (p.hash,p.balance)).run.toList map
        (p => Address(hashToAddress(p._1), p._2))

  }

  

  def getAddressListSummary[A <: Table[_] with BalanceField with HashField with BlockHeightField](richListTable: TableQuery[A], blockHeight: Int): AddressesSummary = transactionDBSession {

    val query = richListTable.filter(_.block_height === blockHeight).sortBy(_.balance.desc)
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
