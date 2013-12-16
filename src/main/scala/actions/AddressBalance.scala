package actions

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 12/2/13
 * Time: 11:38 AM
 * To change this template use File | Settings | File Templates.
 */
import libs._

import scala.slick.session.Database
import Database.threadLocalSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}


class AddressBalance(args:List[String]){
  databaseSession {
    val address = if (args.length > 0 && args(0) != "0") args(0) else "1XPTgDRhN8RFnzniWCddobD9iKZatrvH4"
    val q1 = Q.queryNA[String]("SELECT SUM(o.value) FROM outputs o LEFT OUTER JOIN inputs i ON o.transaction_hash = i.output_transaction_hash AND i.output_index = o.index WHERE o.address = '"+ address+"' AND i.transaction_hash IS NULL")
    println(q1.list.head)

  }
}
