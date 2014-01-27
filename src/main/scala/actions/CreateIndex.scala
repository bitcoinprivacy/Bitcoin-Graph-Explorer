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
class CreateIndex(args:List[String]){
  databaseSession {

    println("Building indexes...")

    val queries:List[String] = List(
    //""" create unique index inpoint on outputs (transaction_hash, `index`)""",
    //  """ create index transaction_hash_o on outputs (transaction_hash)""",

    """ create index address on outputs (address)""",
    //""" create index transaction_hash_i on inputs (transaction_hash)""",
    //""" create unique index outpoint on inputs (output_transaction_hash, `output_index`)""",
    //""" create unique index hash_b on blocks (hash)""",
    """ create index representant on grouped_addresses (representant)""",
    """ create unique index hash_a on grouped_addresses (hash)"""
    )
    for (query <- queries)
    {
      (Q.u + query + ";").execute
      println("Index created: " + query)
    }

  }
}
