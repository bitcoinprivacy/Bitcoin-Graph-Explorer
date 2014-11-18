package core

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/13
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
import util._
import java.io._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import java.lang.System



abstract class AddressClosure(args:List[String])
{
  def adaptTreeIfNecessary(tree:  HashMap[Hash, DisjointSetOfAddresses]):  HashMap[Hash, DisjointSetOfAddresses] = tree
  def createIndexesIfNecessary = { }

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

  def generateTree(start: Int, end: Int): (HashMap[Hash, DisjointSetOfAddresses], Int, Long)  =
  {
    val tree:HashMap[Hash, DisjointSetOfAddresses] = HashMap.empty
    val timeStart = System.currentTimeMillis

    for (i <- start to end by closureReadSize)
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

    (tree, tree.size, System.currentTimeMillis - timeStart)
  }

  def insertValuesIntoTree(databaseResults: HashMap[Hash, Array[Hash]], tree: HashMap[Hash, DisjointSetOfAddresses]) =
  {
    val start = System.currentTimeMillis
    //println("     Inserting values into tree")

    for (t <- databaseResults)
    {
      val dSOAs= t._2 map(a => tree.getOrElseUpdate(a, {DisjointSetOfAddresses(a)}) )

      def union(l:Array[DisjointSetOfAddresses]): Unit = l match
      {
        case Array() =>
        case Array(x) =>
        case ar => ar(0).union(ar(1)) ; union(ar.drop(1))
      }

      union(dSOAs)
    }

    //println("     Values inserted in %s ms" format (System.currentTimeMillis - start))
  }

  def saveTree(tree: HashMap[Hash, DisjointSetOfAddresses]): (Int, Long) =
  {
    val timeStart = System.currentTimeMillis
    var queries: Vector[(Array[Byte], Array[Byte], Option[Long])] = Vector()
    val totalElements = tree.size
    var counter = 0
    var counterTotal = 0
    //println("")
    println("Saving tree to database")
    var counterFinal = 0
    for (value <- tree)
    {
      queries +:= (value._1.array.toArray, value._2.find.address.array.toArray, None)
      counter += 1
      counterTotal += 1
      counterFinal += 1
      if (counter == closureTransactionSize)
      {
        //println("=============================================")
        //println("     Saving until element %s" format (counterTotal))
        saveElementsToDatabase(queries, counter)
        queries = Vector()
        counter = 0
      }
      if (counterFinal % 1000000 == 0) {
        counterFinal = 0
        println("DONE:Saved until element %s, %s µs per element" format (counterTotal, (System.currentTimeMillis - timeStart)/1000, (System.currentTimeMillis - timeStart)*1000/counterTotal))
      }
    }

    //println("=============================================")
    //println("     Saving until element %s" format (counterTotal))
    saveElementsToDatabase(queries, counter)
    //println("=============================================")

    (totalElements, System.currentTimeMillis - timeStart)
  }

  def saveElementsToDatabase(queries: Vector[(Array[Byte], Array[Byte], Option[Long])], counter: Int): Unit =
  {
    val start = System.currentTimeMillis
    //println("     Save transaction of %s ..." format (counter))
    transactionDBSession {
      addresses.insertAll(queries: _*)
    }

    //println("     Saved in %s ms" format (System.currentTimeMillis - start))
  }

  val start = if (args.length>0) args(0).toInt else 0
  val end = if (args.length>1) args(1).toInt else countInputs
  //println("Reading inputs from %s to %s" format (start, end))

  var (tree, countTree, timeTree) = generateTree(start, end)

  val timeStart = System.currentTimeMillis
  tree = adaptTreeIfNecessary(tree)
  timeTree += System.currentTimeMillis - timeStart
  val (countSave, timeSave) = saveTree(tree)

  createIndexesIfNecessary

  println("Total of %s addresses saved in %s s, %s µs per address" format
    (countSave, timeSave / 1000, 1000 * timeSave / (countSave + 1)))
  println("Total of %s addresses processed in %s s, %s µs per address" format
    (countTree, timeTree / 1000, 1000 * timeTree / (countTree + 1)))
  println("/////////////////////////////////////////////")


}
 
 
