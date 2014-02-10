package actions

import libs._

import scala.slick.session.Database
import Database.threadLocalSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

/**
 * Created by yzark on 12/16/13.
 */

// TODO: de you really need me? if yes rewrite me plz, else remove
class AllAddressesBalance(args:List[String]){
  /*databaseSession {
    val values = Q.queryNA[(String,String)]("""SELECT SUM(o.value) as suma, o.address as address FROM outputs o LEFT OUTER JOIN inputs i ON o.transaction_hash = i.output_transaction_hash AND i.output_index = o.`index` where i.transaction_hash IS NULL group by o.address""")

    println("Reading Data...")
    var counter = 0
    var arrQueries:List[String] = List()


    for (value <- values)
    {
      if (counter == 50000)
      {
        println ("Copying elements to Database")
        (Q.u + "BEGIN TRANSACTION").execute
        for (query <- arrQueries)    (Q.u + query).execute
        arrQueries = List()
        (Q.u + "COMMIT TRANSACTION").execute
        counter = 0
      }
      arrQueries = """
      update
        addresses
      set
        balance = """ + value._1 + """
      where
        hash = """" + value._2 + """""""::arrQueries
      counter += 1

    }
    println ("Copying remaining data :D")
    (Q.u + "BEGIN TRANSACTION").execute
    for (query <- arrQueries)    (Q.u + query).execute
    (Q.u + "COMMIT TRANSACTION").execute

    println("Wir sind ultra geil!")

  }*/
}
