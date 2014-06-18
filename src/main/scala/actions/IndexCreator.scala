package actions

import libs._
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import scala.collection.mutable.HashMap
import scala.slick.jdbc.meta.MTable
import scala.collection.JavaConversions._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

/**
 * Created by yzark on 12/16/13.
 */
class IndexCreator(args:List[String])
{
  databaseSession
  {
    println("Building indexes...")
    println("=============================================")
    var timeStart = System.currentTimeMillis;

    val queries:List[String] = List(

      """create index if not exists address on movements (address)""",
      """create index if not exists representant on addresses (representant)""",
      """create unique index if not exists hash on addresses (hash)""",
      """create index if not exists transaction_hash_i on movements (transaction_hash, `index`)""",
      """create index if not exists spent_in_transaction_hash on movements (transaction_hash, spent_in_transaction_hash)""",
      """analyze;"""
    )
    for (query <- queries)
    {
      println("       Index created: " + query)
      (Q.u + query + ";").execute
    }
    println("=============================================")
    println()
    println("/////////////////////////////////////////////")
    println("Indexes created in " + (System.currentTimeMillis - timeStart) + " ms ")
    println("/////////////////////////////////////////////")
    println()
  }
}
