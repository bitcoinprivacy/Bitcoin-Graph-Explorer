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
import scala.slick.jdbc.GetResult
import scala.slick.session.Database
import Database.threadLocalSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.meta.MTable
import scala.collection.mutable.HashMap

class AllAddressesClosure(args:List[String]){

  def generateTree (firstElement: Int, elements: Int): HashMap[String, DisjointSetOfAddresses]  =
  {
    val numberOfSteps = 10
    println("Intializing variables ...")

    val limit = if (args.length > 0) "limit " + args(0) else ""
    val mapDSOA:HashMap[String, DisjointSetOfAddresses] = HashMap.empty
    val mapAddresses:HashMap[String, Array[String]] = HashMap.empty

    var adCon: Int = 1

    var counter = 0

    println("Total rows to read " +elements)
    println("Reading data ...")

    var startTime = System.currentTimeMillis
    val query = """  select i.transaction_hash, o.address from inputs i join outputs o on
        (o.transaction_hash  = i.output_transaction_hash and o.`index` = i.output_index)
        limit """  + firstElement + ','  + elements

    val q2 = Q.queryNA[(String,String)](query)

    for (t <- q2) if (t._2 != "0")
    {
      val list:Array[String] = mapAddresses.getOrElse(t._1, Array()  )

      mapAddresses.update(t._1, list :+ t._2 )

    }

    println("")
    println("Data read in "+ (System.currentTimeMillis - startTime )+" ms"    )


    println("Calculating address dependencies...")
    startTime = System.currentTimeMillis

    for (t <- mapAddresses)
    {
      counter += 1
      val dSOAs= t._2 map(a => mapDSOA.getOrElseUpdate(a, {DisjointSetOfAddresses(a)}) )
      def union(l:Array[DisjointSetOfAddresses]): Unit = l match
      {
        case Array() =>
        case Array(x) =>
        case ar => ar(0).union(ar(1)) ; union(ar.drop(1))
      }
      union(dSOAs)
    }

  mapDSOA

  }

  def adaptTreeToDB(mapDSOA: HashMap[String, DisjointSetOfAddresses]): HashMap[String, DisjointSetOfAddresses] =
  {
    for ( (address, dsoa) <- mapDSOA)
    {
      Q.queryNA[String]("""select representant from addresses where hash= """"+address+"""";""").list match
      {
        case representant::xs =>
          dsoa.find.parent = Some(DisjointSetOfAddresses(representant))
          mapDSOA remove address
        case _  =>
      }

    }

    mapDSOA
  }

  def saveTree(mapDSOA: HashMap[String, DisjointSetOfAddresses]): Unit =
  {
    println("Compiling SQL queries ...")

    val values = (mapDSOA map ( p => ('"'+ p._1 + '"' , '"' + p._2.find.address +'"', 0.toDouble))).toList

    println("Copying results to the database...")

    (Q.u + "BEGIN TRANSACTION;").execute

    for (value <- values )
      (Q.u + """insert into addresses values """+ value.toString +""";""").execute
    (Q.u + "COMMIT TRANSACTION;").execute
  }


  databaseSession  {
    val start = args(0).toInt
    val end = args(1).toInt

    var i = start

    for (i <- start to end by stepClosure)
    {
      println("Closuring inputs from " + i + " to " + ( i + stepClosure ) )
      saveTree(adaptTreeToDB(generateTree(i, stepClosure)))
    }

    println("Wir sind sehr geil!!!")
  }
}
