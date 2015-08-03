package actions

import core._
import java.util.Calendar
import scala.slick.driver.JdbcDriver.simple._
import scala.slick.jdbc._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util._

object FastAddressClosure extends AddressClosure {
  def generateTree =
  {
    val step = conf.getInt("closureReadSize")

    val movementCount = countInputs

    def txListQuery(start: ConstColumn[Long]) = {
      val emptyArray = Hash.zero(0).array.toArray
      transactionDBSession {
        for (q <- movements.drop(start).take(step).filter(_.address =!= emptyArray))
          yield (q.spent_in_transaction_hash, q.address)
        // in order to read quickly from db, we need to read in the order of insertion
      }
    }
    val txList = Compiled(txListQuery _)

    @annotation.tailrec
    def addNextRows(start: Long, tree: DisjointSets[Hash]): DisjointSets[Hash] = {
      println("reading " + step + " elements from " + start)
      val txAndAddressList = transactionDBSession { txList(start).run.toVector }
      txAndAddressList.lastOption match
      {
        case None => tree // empty List, we are finished
        case Some((newStartTx,_)) =>

          val addressesPerTxMap = txAndAddressList.groupBy(_._1)

          def hashList(addressesPerTxMap: Map[Array[Byte],Seq[(Array[Byte],Array[Byte])]]) = addressesPerTxMap.values map (_ map (p=>Hash(p._2)))
          println("folding and merging " + Calendar.getInstance().getTime() )
          def newTree(hashList: Iterable[Seq[Hash]]) = hashList.foldLeft (tree) ((t,l) => insertInputsIntoTree(l,t))

          if (start+step < movementCount) // not finished. remove last (possibly splitted) tx and recurse
            addNextRows(start+step-addressesPerTxMap(newStartTx).length, newTree(hashList(addressesPerTxMap - newStartTx)))
          else // finished
            newTree(hashList(addressesPerTxMap))
      }

    }

    addNextRows(0, new DisjointSets[Hash](collection.immutable.HashMap.empty))
   }
}


