package actions

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/13
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
import bddb._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.GetResult
import scala.slick.session.Database
import Database.threadLocalSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.meta.MTable
import scala.collection.mutable.HashMap

class AllAddressesClosure(args:List[String]){
  Database.forURL(
    url = "jdbc:mysql://localhost/bitcoin",
    driver = "com.mysql.jdbc.Driver",
    user = "root",
    password = "12345"

  )  withSession {
    var mapDSOA:HashMap[String, DisjointSetOfAddresses] = HashMap.empty


    val q2 = Q.queryNA[String]("""
      select group_concat(o.address) as addresses from outputs o join inputs i on o.transaction_hash = i.output_transaction_hash and i.output_index = o.index group by i.transaction_hash
    """)
    println("Creating table...")
    var tableList = MTable.getTables.list;
    var tableMap = tableList.map{t => (t.name.name, t)}.toMap;
    if (tableMap.contains("grouped_addresses"))
      (GroupedAddresses.ddl).drop
    (GroupedAddresses.ddl).create
    println("Calculating address dependencies...")
    for (t <- q2.list)
    {

      val addresses = t.split(",").toList
      //println(addresses)
      val dSOAs= addresses filterNot (_=="0") map(a => mapDSOA.getOrElseUpdate(a, {DisjointSetOfAddresses(a)}) )
      def union(l:List[DisjointSetOfAddresses]): Unit = l match
      {
        case Nil =>
        case x::Nil =>
        case x::y::xs => x.union(y) ; union(y::xs)
      }
      union(dSOAs)
   }
   println("Copying results to the database...")
   GroupedAddresses.insertAll((mapDSOA map ( p => (p._1,p._2.find.address) )).toSeq:_*)
   println("Wir sind geil!!!")
  }
}
