package actions

import util._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.slick.jdbc.{StaticQuery => Q}

object CreateAddressIndexes {

  println("DEBUG: Creating indexes ...")
  val time = System.currentTimeMillis

  transactionDBSession {
    for (query <- List(
      "create index representant on addresses (representant)",
        "create unique index hash on addresses (hash)"
    ))
    {
      Q.updateNA(query).execute
      println("DEBUG: Finished "+ query)
    }


   }

  println("DONE: Indexes created in %s s" format (System.currentTimeMillis - time)/1000)

}
