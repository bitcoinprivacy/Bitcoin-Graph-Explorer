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

    if (Q.queryNA[Int]("""select count(*) from outputs where transaction_hash = "d5d27987d2a3dfc724e359870c6644b40e497bdc0589a033220fe15429d88599"""").list.head > 1)
      Q.queryNA[String]("""delete from outputs where transaction_hash = "d5d27987d2a3dfc724e359870c6644b40e497bdc0589a033220fe15429d88599" limit 1""")
    if (Q.queryNA[Int]("""select count(*) from outputs where transaction_hash = "e3bf3d07d4b0375638d5f1db5255fe07ba2c4cb067cd81b84ee974b6585fb468"""").list.head > 1)
      Q.queryNA[String]("""delete from outputs where transaction_hash = "e3bf3d07d4b0375638d5f1db5255fe07ba2c4cb067cd81b84ee974b6585fb468" limit 1""")
    println("Building indexes...")

    (Q.u + """ create unique index inpoint on outputs (transaction_hash, `index`)""").execute
    (Q.u + """ create index transaction_hash on outputs (transaction_hash)""").execute
    (Q.u + """ create index address on outputs (address)""").execute
    (Q.u + """ create index transaction_hash on inputs (transaction_hash)""").execute
    (Q.u + """ create unique index outpoint on inputs (output_transaction_hash, `output_index`)""").execute
    (Q.u + """ create unique index hash on blocks (hash)""").execute
    (Q.u + """ create index representant on grouped_addresses (representant)""").execute
    (Q.u + """ create unique index hash on grouped_addresses (hash)""").execute
  }
}
