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
  databaseSession  {
	val numberOfSteps = 10
  println("Intializing variables ...")

  val limit = if (args.length > 0) "limit " + args(0) else ""
  val mapDSOA:HashMap[Int, DisjointSetOfAddresses] = HashMap.empty
  val mapAddresses:HashMap[String, Array[Int]] = HashMap.empty
  var adMap: HashMap[String,Int] = HashMap.empty

  var adCon: Int = 1

  var counter = 0

  val q1 = Q.queryNA[Long](
        """
        select
          count(*)
        from
          inputs
          """ + limit
  )
  var max = 0.toLong
  for (t <- q1)
	  max = t
  println("Total rows to read "+max)
  println("Reading data ...")

  var startTime = System.currentTimeMillis


  var step = max / numberOfSteps
  var position = 0.toLong

  var pos = 0
  while (position < max && step > 0)
  {
    println(pos)
    pos = pos+1

    if (position + step > max)
		  step = max - position
	
	  val query = """  select i.transaction_hash, o.address from inputs i join outputs o on
          (o.transaction_hash  = i.output_transaction_hash and o.`index` = i.output_index)
          limit """  + position + """,""" +step

    println(query)
    position = position + step
	
	  val q2 = Q.queryNA[(String,String)](query)

    for (t <- q2) if (t._2 != "0")
    {
      val t2 = adMap.getOrElseUpdate(t._2, { adCon += 1; adCon } )

      val list:Array[Int] = mapAddresses.getOrElse(t._1, Array()  )

      mapAddresses.update(t._1, list :+ t2 )

    }
    //print("/")
}
	  //println("admap size " + globalInstr.getObjectSize(adMap))
    //println("mapaddresses size "+ globalInstr.getObjectSize(mapAddresses))
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
   println("Dependences calculated in "+ (System.currentTimeMillis - startTime) +" ms")
   println("Compiling SQL queries ...")

   val adMapReverse = adMap map (_.swap)
   val values = (mapDSOA map ( p => ('"'+ adMapReverse(p._1) +'"','"'+adMapReverse(p._2.find.address) +'"', 0.toDouble))).toList

  println("Copying results to the database...")
  startTime = System.currentTimeMillis
  (Q.u + "BEGIN TRANSACTION;").execute

  for (value <- values )
    (Q.u + """insert into grouped_addresses values """+ value.toString +""";""").execute
  (Q.u + "COMMIT TRANSACTION;").execute

   println("Data copied in "+ (System.currentTimeMillis - startTime) +" ms")
   println("Wir sind sehr geil!!!")
  }
}
