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
    val mapDSOA:HashMap[String, DisjointSetOfAddresses] = HashMap.empty
    val mapAddresses:HashMap[String, List[String]] = HashMap.empty
    var counter = 0
    val q2 = Q.queryNA[(String,String)](
      """
      select
        IFNULL(i.transaction_hash,"NULL"),
        o.address
      from
        outputs o left outer join
        inputs i on
          (
            i.output_transaction_hash = o.transaction_hash  and
            i.output_index = o.index
          )
      where
        o.address != "0"
      ;
      """
    )
    for (t <- q2)
    {
      val list = mapAddresses.getOrElse(t._1, { Nil }  )

      mapAddresses.update(t._1, t._2::list)
    }


       //order by i.transaction_hash
    println("Creating table...")
    var tableList = MTable.getTables.list;
    var tableMap = tableList.map{t => (t.name.name, t)}.toMap;
    if (tableMap.contains("grouped_addresses"))
      (GroupedAddresses.ddl).drop
    (GroupedAddresses.ddl).create
    println("Calculating address dependencies...")

    //println(mapAddresses(""))
    // TODO: avoid duplicate addresses
    println("Total transaction addresses: "+mapAddresses.size)
    if (mapAddresses.contains("NULL"))
    {
      for (a <- mapAddresses("NULL"))
        mapDSOA.getOrElseUpdate(a, {DisjointSetOfAddresses(a)})

      mapAddresses.remove("NULL")
    }



    for (t <- mapAddresses)
    {
      //println("Reading element "+counter)
      counter += 1


      val dSOAs= t._2 filterNot (_=="0") map(a => mapDSOA.getOrElseUpdate(a, {DisjointSetOfAddresses(a)}) )

      def union(l:List[DisjointSetOfAddresses]): Unit = l match
      {
        case Nil =>
        case x::Nil =>
        case x::y::xs => x.union(y) ; union(y::xs)
      }
      union(dSOAs)
   }
   println("Copying results to the database...")
   GroupedAddresses.insertAll((mapDSOA map ( p => (p._1,p._2.find.address, 0.toDouble) )).toSeq:_*)
   println("Wir sind geil!!!")
  }
}
