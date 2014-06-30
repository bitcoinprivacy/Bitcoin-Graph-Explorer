package actions

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/13
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
import libs._
import java.io._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.session.Database
import Database.threadLocalSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import libs.DisjointSetOfAddresses
import scala.Some

class AddressesClosurer(args:List[String])
{
  def getAddressesFromMovements(firstElement: Int, elements: Int): HashMap[Hash, Array[Hash]] =
  {
    // weird trick to allow slick using Array Bytes
    implicit val GetByteArr = GetResult(r => r.nextBytes())
    var nullHash = Hash.zero(1).toString
    var zeroHash = Hash.zero(20)
    val timeStart = System.currentTimeMillis
    println("     Reading until input %s" format (firstElement + elements))
    val mapAddresses:HashMap[Hash, Array[Hash]] = HashMap.empty
    val movements = for
    {
      o <- Outputs.filter(!_.spent_in_transaction_hash.equals(Hash.zero(1))).
          filter(!_.spent_in_transaction_hash.equals(Hash.zero(20))).
            filter(!_.address.equals(Hash.zero(1))).
              drop(firstElement).
                take(elements)
    }
    yield(o.spent_in_transaction_hash, o.address)

    for (m <- movements)
    {
        mapAddresses.update(
          Hash(m._2),
          mapAddresses.getOrElse(Hash(m._1), Array()):+
            Hash(m._1)
        )
    }

    println("     Read elements in %s ms" format (System.currentTimeMillis - timeStart))

    mapAddresses
  }

  def generateTree(start: Int, end: Int): (HashMap[Hash, DisjointSetOfAddresses], Int, Long)  =
  {
    val tree:HashMap[Hash, DisjointSetOfAddresses] = HashMap.empty
    val timeStart = System.currentTimeMillis

    for (i <- start to end by closureReadSize)
    {
      println("=============================================")
      val amount = if (i + closureReadSize > end) end - i else closureReadSize
      val (databaseResults) = getAddressesFromMovements(i, amount)
      insertValuesIntoTree(databaseResults, tree)
    }

    println("=============================================")

    (tree, tree.size, System.currentTimeMillis - timeStart)
  }

  def insertValuesIntoTree(databaseResults: HashMap[Hash, Array[Hash]], tree: HashMap[Hash, DisjointSetOfAddresses]) =
  {
    val start = System.currentTimeMillis
    println("     Inserting values into tree")

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

    println("     Values inserted in %s ms" format (System.currentTimeMillis - start))
  }

  // we will use later for updates
  def adaptTreeToDB(mapDSOA: HashMap[Hash, DisjointSetOfAddresses]): HashMap[Hash, DisjointSetOfAddresses] =
  {
    val timeStart = System.currentTimeMillis
    println("     Adapting tree to database ...")

    val query = "select hash, representant from addresses"
    // weird trick to allow slick using Array Bytes
    implicit val GetByteArr = GetResult(r => r.nextBytes())
    val q2 = Q.queryNA[(Array[Byte],Array[Byte])](query)

    for (pair <- q2)
    {
      val (hash, representant) = pair
      val address = Hash(hash)
      if (mapDSOA.contains(address))
      {
        mapDSOA(address).find.parent = Some(DisjointSetOfAddresses(Hash(representant)))
        mapDSOA remove address
      }
    }

    /*for ( (address, dsoa) <- mapDSOA)
    {
      // weird trick to allow slick using Array Bytes
      implicit val GetByteArr = GetResult(r => r.nextBytes())
      Q.queryNA[Array[Byte]]("select representant from addresses where hash= %s;" format (address)).list match
      {
        case representant::xs =>
          dsoa.find.parent = Some(DisjointSetOfAddresses(Hash(representant)))
          mapDSOA remove address
        case _  =>
      }
    }*/

    println("     Tree adapted in %s ms" format (System.currentTimeMillis - timeStart))

    mapDSOA
  }

  def saveTree(tree: HashMap[Hash, DisjointSetOfAddresses]): (Int, Long) =
  {
    val timeStart = System.currentTimeMillis
    var queries: Vector[(Array[Byte], Array[Byte], Double)] = Vector()
    val totalElements = tree.size
    var counter = 0
    var counterTotal = 0
    println("")
    println("Saving tree to database")

    for (value <- tree)
    {
      queries +:= (value._1.array.toArray, value._2.find.address.array.toArray, 0.0)
      counter += 1
      counterTotal += 1
      if (counter == closureTransactionSize)
      {
        println("=============================================")
        println("     Saving until element %s" format (counterTotal))
        saveElementsToDatabase(queries, counter)
        queries = Vector()
        counter = 0
      }
    }

    println("=============================================")
    println("     Saving until element %s" format (counterTotal))
    saveElementsToDatabase(queries, counter)
    println("=============================================")

    (totalElements, System.currentTimeMillis - timeStart)
  }

  def saveElementsToDatabase(queries: Vector[(Array[Byte], Array[Byte], Double)], counter: Int): Unit =
  {
    val start = System.currentTimeMillis
    println("     Save transaction of %s ..." format (counter))
    Addresses.insertAll(queries: _*)
    //(Q.u + "BEGIN TRANSACTION;").execute
    //for (query <- queries) Addresses.insertAll((query._1.array.toArray, query._2.array.toArray, 0.0))

    //(Q.u + "insert into addresses VALUES " +
    // "(" + query._1.toString + "," + query._2.toString + ", 0 );").execute
    //(Q.u + "COMMIT TRANSACTION;").execute
    println("     Saved in %s ms" format (System.currentTimeMillis - start))
  }

  var outputs: List[String] = List.empty

  transactionsDBSession
  {
    val start = if (args.length>0) args(0).toInt else 0
    val end = if (args.length>1) args(1).toInt else countInputs
    startLogger("closure_"+start+"_"+end);
    println("Reading inputs from %s to %s" format (start, end))

    var (tree, countTree, timeTree) = generateTree(start, end)

    if (start == 0 )new File(addressesDatabaseFile).delete
    addressesDBSession
    {
      if (start == 0) // Do only if we start closuring
      {
        (Addresses.ddl).create
      }
      else
      {
        tree = adaptTreeToDB(tree)
      }
      val (countSave, timeSave) = saveTree(tree)

      var clockIndex = System.currentTimeMillis
      println("Creating indexes ...")
      //(Q.u + "create index if not exists representant on addresses (representant)").execute
      //(Q.u + "create unique index if not exists hash on addresses (hash)").execute
      println("=============================================")
      println("")
      clockIndex = System.currentTimeMillis - clockIndex
      println("/////////////////////////////////////////////")
      println("Indices created in %s s" format (clockIndex/1000))
      println("Total of %s addresses saved in %s s, %s µs per address" format
        (countSave, timeSave/1000, 1000*timeSave/(countSave+1)))
      println("Total of %s addresses processed in %s s, %s µs per address" format
        (countTree, timeTree/1000, 1000*timeTree/(countTree+1)))
      println("/////////////////////////////////////////////")
    }


  }

  stopLogger
}