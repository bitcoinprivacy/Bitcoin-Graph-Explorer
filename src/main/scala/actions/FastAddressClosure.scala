package actions

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._
import core._

object FastAddressClosure extends AddressClosure {
  def generateTree =
  {
    println("running groupBy")
    val txList = movements.groupBy(_.transaction_hash).map{
      case(tx,group) => tx
    }.iterator

    println("getting addressLists")

    val addressList = for {tx <- txList}
    yield movements.filter(_.transaction_hash === tx).map(_.address).iterator.toIterable
    val hashList = addressList map (_ map (Hash(_)))
    
    println("building tree")
    hashList.foldLeft(new DisjointSets[Hash](scala.collection.immutable.HashMap.empty))((t,l) => insertInputsIntoTree(l,t))
  }
}
