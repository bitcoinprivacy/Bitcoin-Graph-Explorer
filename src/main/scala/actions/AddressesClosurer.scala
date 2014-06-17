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
  var representantsCount = 0
  var start = 0
  var end = 0
  def generateTree (firstElement: Int, elements: Int): HashMap[Hash, DisjointSetOfAddresses]  =
  {
    val mapDSOA:HashMap[Hash, DisjointSetOfAddresses] = HashMap.empty
    val mapAddresses:HashMap[Hash, Array[Hash]] = HashMap.empty
    var startTime = System.currentTimeMillis

    val query = """ select spent_in_transaction_hash, address from
        movements where spent_in_transaction_hash NOT NULL and address NOT NULL limit """ + firstElement + ',' + elements + """ ; """
        


    implicit val GetByteArr = GetResult(r => r.nextBytes())
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

    println("       Data read in "+ (System.currentTimeMillis - startTime )+" ms")
    println("       Calculating address dependencies...")
    startTime = System.currentTimeMillis

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

    println("       Dependencies calculated in " + (System.currentTimeMillis - startTime) + " ms ")
    mapDSOA

  }

  def adaptTreeToDB(mapDSOA: HashMap[Hash, DisjointSetOfAddresses]): HashMap[Hash, DisjointSetOfAddresses] =
  {
    for ( (address, dsoa) <- mapDSOA)
    {
      // weird trick to allow slick using Array Bytes
      implicit val GetByteArr = GetResult(r => r.nextBytes())
      Q.queryNA[Array[Byte]]("select representant from addresses where hash= "+address+";").list match
      {
        case representant::xs =>
          dsoa.find.parent = Some(DisjointSetOfAddresses(Hash(representant)))
          mapDSOA remove address
        case _  =>
      }
    }

    mapDSOA
  }

  def saveTree(mapDSOA: HashMap[Hash, DisjointSetOfAddresses]): Unit =
  {
    val values = (mapDSOA map ( p => p._1 + " , " + p._2.find.address + ", " + 0.toDouble)).toList
    val timeStart = System.currentTimeMillis
    println("       Copying results to the database...")
    (Q.u + "BEGIN TRANSACTION;").execute

    for (value <- values )
    {
      (Q.u + "insert into addresses (`hash`, `representant`, `balance`) values ("+ value +");").execute
    }
    println("       Results copied in " + (System.currentTimeMillis - timeStart) + " ms ")
    (Q.u + "COMMIT TRANSACTION;").execute
  }

  databaseSession
  {
    println("Calculating closure of existing addresses")
    start = if (args.length>0) args(0).toInt else 0
    end = if (args.length>1) args(1).toInt else countInputs
    val timeStart = System.currentTimeMillis

    for (i <- start to end by stepClosure)
    {
      val startTime = System.currentTimeMillis
      println("=============================================")
      println("       Closuring inputs from " + i + " to " + ( i + stepClosure ) + " / " + end )
      println("       Elements closured in " + (System.currentTimeMillis - startTime) + " ms ")
      saveTree(adaptTreeToDB(generateTree(i, stepClosure)))
    }


    println("=============================================")
    println
    println("/////////////////////////////////////////////")
    println("Addresses closured in " + (System.currentTimeMillis - timeStart) + " ms ")
    println("/////////////////////////////////////////////")
    println

  }
}
