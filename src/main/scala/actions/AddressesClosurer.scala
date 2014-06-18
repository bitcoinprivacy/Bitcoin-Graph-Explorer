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
import scala.slick.jdbc.meta.MTable
import scala.collection.mutable.HashMap

class AddressesClosurer(args:List[String]){

  def getAddressesFromMovements(firstElement: Int, elements: Int): HashMap[Hash, Array[Hash]] =
  {
    // weird trick to allow slick using Array Bytes
    implicit val GetByteArr = GetResult(r => r.nextBytes())
    val timeStart = System.currentTimeMillis
    println("     Reading %s inputs" format (elements))
    val mapAddresses:HashMap[Hash, Array[Hash]] = HashMap.empty
    val query = "select spent_in_transaction_hash as a, address as b from movements where a NOT NULL and b NOT NULL limit %s, %s;"
    val q2 = Q.queryNA[(Array[Byte],Array[Byte])](query format (firstElement, elements))
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

    val timeRequired = System.currentTimeMillis - timeStart;
    println("     Read elements in %s ms" format (timeRequired))

    mapAddresses
  }

  def generateTree (mapAddresses: HashMap[Hash, Array[Hash]]): HashMap[Hash, DisjointSetOfAddresses]  =
  {
    val mapDSOA:HashMap[Hash, DisjointSetOfAddresses] = HashMap.empty
    val timeStart = System.currentTimeMillis
    println("     Generating dependence tree ...")

    for (t <- mapAddresses)
    {
      val dSOAs= t._2 map(a => mapDSOA.getOrElseUpdate(a, {DisjointSetOfAddresses(a)}) )

      def union(l:Array[DisjointSetOfAddresses]): Unit = l match
      {
        case Array() =>
        case Array(x) =>
        case ar => ar(0).union(ar(1)) ; union(ar.drop(1))
      }

      union(dSOAs)
    }

    println("     Dependencies generated in %s ms" format (System.currentTimeMillis - timeStart))

    mapDSOA
  }

  def adaptTreeToDB(mapDSOA: HashMap[Hash, DisjointSetOfAddresses]): HashMap[Hash, DisjointSetOfAddresses] =
  {
    // weird trick to allow slick using Array Bytes
    implicit val GetByteArr = GetResult(r => r.nextBytes())
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

    val timeTotal = System.currentTimeMillis - timeStart
    println("     Tree adapted in %s ms" format (timeTotal))

    mapDSOA
  }

  def saveTree(mapDSOA: HashMap[Hash, DisjointSetOfAddresses]): Int =
  {
    val timeStart = System.currentTimeMillis
    println("     Saving tree to database ...")
    val values = (mapDSOA map ( p => p._1 + " , " + p._2.find.address + ", " + 0.toDouble)).toList
    var count = 0
    (Q.u + "BEGIN TRANSACTION;").execute

    for (value <- values )
    {
      count += 1
      (Q.u + ("insert into addresses (`hash`, `representant`, `balance`) values (%s)" format value)).execute
    }

    (Q.u + "COMMIT TRANSACTION;").execute
    val timeInvested = System.currentTimeMillis - timeStart
    println("     Tree saved in %s ms" format (timeInvested))

    count
  }

  databaseSession
  {
    val timeStart = System.currentTimeMillis
    println("Calculating closure of existing addresses")
    (Q.u + "delete from addresses;").execute
    val start = if (args.length>0) args(0).toInt else 0
    val end = if (args.length>1) args(1).toInt else countInputs
    var counterTotal = 0

    for (i <- start to end by stepClosure)
    {
      val timeStart = System.currentTimeMillis
      val amount = if (i + stepClosure > end) end - i else stepClosure
      println("=============================================")
      println("     Closuring using inputs from %s to %s" format (i, i + amount))
      val counter = saveTree(adaptTreeToDB(generateTree(getAddressesFromMovements(i, amount))))
      counterTotal += counter
      val timeTotal = System.currentTimeMillis - timeStart
      println("     Closured %s elements in %s s" format (counter, timeTotal / 1000))
    }

    val timeTotal = (System.currentTimeMillis - timeStart)
    val timePerAddress = if (counterTotal != 0) 1000*timeTotal/counterTotal else timeTotal
    println("=============================================")
    println
    println("/////////////////////////////////////////////")
    println("Total of %s addresses processed in %s s, %s µs per address" format (counterTotal, timeTotal/1000, timePerAddress));
    println("/////////////////////////////////////////////")
    println
  }
}
