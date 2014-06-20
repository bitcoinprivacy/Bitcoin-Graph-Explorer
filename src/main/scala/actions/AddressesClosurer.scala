package actions

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/13
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
import libs._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.session.Database
import Database.threadLocalSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import javax.sound.midi.SysexMessage

class AddressesClosurer(args:List[String])
{
  def getAddressesFromMovements(firstElement: Int, elements: Int): HashMap[Hash, Array[Hash]] =
  {
    // weird trick to allow slick using Array Bytes
    implicit val GetByteArr = GetResult(r => r.nextBytes())
    val timeStart = System.currentTimeMillis
    println("     Reading until input %s" format (firstElement + elements))
    val mapAddresses:HashMap[Hash, Array[Hash]] = HashMap.empty
    val query = "select spent_in_transaction_hash as a, address as b from movements where a " +
      "NOT NULL and b NOT NULL limit %s, %s;" format (firstElement, elements)
    val q2 = Q.queryNA[(Array[Byte],Array[Byte])](query)
    val emptyArray = Hash.zero(20)

    for (q <- q2)
    {
      val t = (Hash(q._1), Hash(q._2))

      if (t._2 != emptyArray)
      {
        val list:Array[Hash] = mapAddresses.getOrElse(t._1, Array()  )
        mapAddresses.update(t._1, list :+ t._2 )
      }
    }

    println("     Read elements in %s ms" format (System.currentTimeMillis - timeStart))

    mapAddresses
  }

  def generateTree(start: Int, end: Int): (HashMap[Hash, DisjointSetOfAddresses], Int, Long)  =
  {
    val tree:HashMap[Hash, DisjointSetOfAddresses] = HashMap.empty
    val timeStart = System.currentTimeMillis

    for (i <- start to end by stepClosure)
    {
      println("=============================================")
      val amount = if (i + stepClosure > end) end - i else stepClosure
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

    for ( (address, dsoa) <- mapDSOA)
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
    }

    println("     Tree adapted in %s ms" format (System.currentTimeMillis - timeStart))

    mapDSOA
  }

  def saveTree(tree: HashMap[Hash, DisjointSetOfAddresses]): (Int, Long) =
  {
    val timeStart = System.currentTimeMillis
    var queries: List[String] = List.empty
    val totalElements = tree.size
    var counter = 0
    var counterTotal = 0
    println
    println("Saving tree to database")

    for (value <- tree)
    {
      queries = ("insert into addresses (`hash`, `representant`, `balance`) values (%s, %s, %s)" format
        (value._1, value._2.find.address, 0))::queries
      counter += 1
      counterTotal += 1
      if (counter == stepClosure)
      {
        println("=============================================")
        println("     Saving until element %s" format (counterTotal))
        saveElementsToDatabase(queries, counter)
        queries = List.empty
        counter = 0
      }
    }

    println("=============================================")
    println("     Saving until element %s" format (counterTotal))
    saveElementsToDatabase(queries, counter)
    println("=============================================")

    (totalElements, System.currentTimeMillis - timeStart)
  }

  def saveElementsToDatabase(queries: List[String], counter: Int): Unit =
  {
    val start = System.currentTimeMillis
    println("     Save transaction of %s ..." format (counter))
    (Q.u + "BEGIN TRANSACTION;").execute
    for (query <- queries) (Q.u + query).execute
    (Q.u + "COMMIT TRANSACTION;").execute
    println("     Saved in %s ms" format (System.currentTimeMillis - start))
  }

  var outputs: List[String] = List.empty

  databaseSession
  {
    println("Calculating closure of existing addresses ...")
    println("Dropping and recreating address database")

    val start = if (args.length>0) args(0).toInt else 0
    val end = if (args.length>1) args(1).toInt else countInputs

    if (start == 0) // Do only if we start closuring
    {
      (Addresses.ddl).drop
      (Addresses.ddl).create
    }
    println("Reading inputs from %s to %s" format (start, end))

    var (tree, countTree, timeTree) = generateTree(start, end)
    if (start != 0) tree = adaptTreeToDB(tree)
    val (countSave, timeSave) = saveTree(tree)
    outputs = ("Total of %s addresses saved in %s s, %s µs per address" format
      (countSave, timeSave/1000, 1000*timeSave/countSave))::outputs
    outputs = ("Total of %s addresses processed in %s s, %s µs per address" format
      (countTree, timeTree/1000, 1000*timeTree/countTree))::outputs
  }

  // We perform that here since IndexCreator call a databaseSession himself
  println
  new IndexCreator(List(
    "create index if not exists representant on addresses (representant)",
    "create unique index if not exists hash on addresses (hash)"/*,
    "analyze"*/
  ))
  for (line <- outputs) println(line)
  println("/////////////////////////////////////////////")
  println
}