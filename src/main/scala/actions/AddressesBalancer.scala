package actions

import libs._

import scala.slick.session.Database
import Database.threadLocalSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

/**
 * Created by yzark on 12/16/13.
 */

// TODO: do you really need me? if yes rewrite me plz, else remove
class AddressesBalancer(args:List[String])
{
  var timeStart = System.currentTimeMillis
  databaseSession
  {
    implicit val GetByteArr = GetResult(r => r.nextBytes())
    
    val values = Q.queryNA[(Double, Array[Byte])]("SELECT SUM(value) as suma, address FROM movements m where spent_in_transaction_hash IS NULL group by address")

    println("Calculating balance...")
    var counter = 0
    var arrQueries:List[String] = List()


    for (value <- values)
    {
      if (counter == stepBalance)
      {
        println("=============================================")
        var timeStart = System.currentTimeMillis
        println ("       Copying " + arrQueries.length + " elements to the Database")

        (Q.u + "BEGIN TRANSACTION").execute
        for (query <- arrQueries)    (Q.u + query).execute
        arrQueries = List()
        (Q.u + "COMMIT TRANSACTION").execute
        println ("       Copied elements in " + (System.currentTimeMillis - timeStart) + " ms ")
        counter = 0
      }
      
      arrQueries = "update addresses set balance = " + value._1 + " where hash = " + Hash(value._2)::arrQueries
      counter += 1
    }

    println("=============================================")
    var timeStart = System.currentTimeMillis
    println ("       Copying " + arrQueries.length + " elements to the Database")
    (Q.u + "BEGIN TRANSACTION").execute
    for (query <- arrQueries)    (Q.u + query).execute
    (Q.u + "COMMIT TRANSACTION").execute
    println ("       Copied elements in " + (System.currentTimeMillis - timeStart) + " ms ")
    println("=============================================")
    println
    println("/////////////////////////////////////////////")
    println("Balance generated in " + (System.currentTimeMillis - timeStart) + " ms ")
    println("/////////////////////////////////////////////")
    println

  }
}
