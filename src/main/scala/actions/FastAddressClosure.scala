import core._ 
import util._
import java.io._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

object FastAddressClosure extends AddressClosure
{
  override def createIndexesIfNecessary = transactionDBSession
  {
    var clockIndex = System.currentTimeMillis
    println("DEBUG:Creating indexes ...")
    (Q.u + "create index if not exists representant on addresses (representant)").execute
    (Q.u + "create unique index if not exists hash on addresses (hash)").execute
    println("=============================================")
    println("")
    clockIndex = System.currentTimeMillis - clockIndex
    println("/////////////////////////////////////////////")
    println("DONE:Indices created in %s s" format (clockIndex / 1000))
  }

  def getAddressesFromMovements(firstElement: Int, elements: Int): HashMap[Hash, Array[Hash]] =
  {
    val timeStart = System.currentTimeMillis
    //println("     Reading until input %s" format (firstElement + elements))
    val mapAddresses:HashMap[Hash, Array[Hash]] = HashMap.empty
    val emptyArray = Hash.zero(0).array.toArray

    transactionDBSession {
      val queried = for {
        q <- movements.drop(firstElement).take(elements)
          .filter(q => q.spent_in_transaction_hash.isDefined && q.address.isDefined)
          .filter(_.spent_in_transaction_hash =!= emptyArray)
      } yield (q.spent_in_transaction_hash, q.address)

      for {q <- queried
           sTx <- q._1
           ad <- q._2} {
        val spentInTx = Hash(sTx)
        val addr = Hash(ad)

        assert(addr != Hash(emptyArray), "=!= doesn't work")
        val list: Array[Hash] = mapAddresses.getOrElse(spentInTx, Array())
        mapAddresses.update(spentInTx, list :+ addr)
      }
    }
    //println("     Read elements in %s ms" format (System.currentTimeMillis - timeStart))

    mapAddresses
  }

  def generateTree: (HashMap[Hash, DisjointSetOfAddresses])  =
  {
    val tree:HashMap[Hash, DisjointSetOfAddresses] = HashMap.empty
    val timeStart = System.currentTimeMillis

    val end = countInputs

    for (i <- 0 to end by closureReadSize)
    {
      //println("=============================================")
      val amount = if (i + closureReadSize > end) end - i else closureReadSize
      insertValuesIntoTree(getAddressesFromMovements(i, amount), tree)
      println("DONE:Loaded until element %s in %s s, %s µs per element"
        format
        (i+amount,
          (System.currentTimeMillis - timeStart)/1000,
          (System.currentTimeMillis - timeStart)*1000/(amount+i)))
    }

    //println("=============================================")

    tree
  }
}
