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

    
    for (query <- args)
    {
      (Q.u + query + ";").execute
      println("       Index created: " + query)
    }
    println("=============================================")
    println()
    println("/////////////////////////////////////////////")
    println("Indexes created in " + (System.currentTimeMillis - timeStart) + " ms ")
    println("/////////////////////////////////////////////")
    println()
  }
}
