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

      """ create index address on outputs (address)""",
      """ create index transaction_hash_i on inputs (transaction_hash)""",
      """ create index representant on addresses (representant)""",
      """ create unique index hash_a on addresses (hash)""",
      """ CREATE INDEX pk_tab1 ON outputs(transaction_hash, `index`); """,
      """ pragma writable_schema=1;""",
      """ UPDATE sqlite_master SET name='sqlite_autoindex_tab1_1',sql=null WHERE name='pk_tab1';""",
      """ UPDATE sqlite_master SET sql='CREATE TABLE outputs(transaction_hash text, address text, `index` int, value double, primary key(transaction_hash, `index`))' WHERE name='outputs';""",
      """ UPDATE sqlite_master SET sql='CREATE TABLE inputs(output_transaction_hash, output_index, transaction_hash, FOREIGN KEY(output_transaction_hash, output_index) REFERENCES outputs(transaction_hash, `index`))' WHERE name='inputs';"""
    )
    for (query <- queries)
    {
      (Q.u + query + ";").execute
      println("Index created: " + query)
    }

  }
}
