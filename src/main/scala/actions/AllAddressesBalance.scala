package actions

import libs._

import scala.slick.session.Database
import Database.threadLocalSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

/**
 * Created by yzark on 12/16/13.
 */
class AllAddressesBalance(args:List[String]){
  databaseSession {
    val values = Q.queryNA[(String,String)]("""SELECT SUM(o.value) as suma, o.address as address FROM outputs o LEFT OUTER JOIN inputs i ON o.transaction_hash = i.output_transaction_hash AND i.output_index = o.`index` where i.transaction_hash IS NULL group by o.address""")

    println("balances calculated. now writing ...")
    (Q.u + "BEGIN TRANSACTION").execute
    for (value <- values)
    (Q.u + """
      update
        grouped_addresses
      set
        balance = """ + value._1 + """
      where
        hash = """" + value._2 + """""""
    ).execute

    (Q.u + "COMMIT TRANSACTION").execute
    println("Wir sind geil!")

  }
}
