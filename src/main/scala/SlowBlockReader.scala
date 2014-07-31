import libs._ // for blocks db and longestChain
import com.google.bitcoin.core._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

trait SlowBlockReader extends BlockSource {

  transactionsDBSession {

    val longestChain = getLongestBlockChainHashSet
    def blockHashQuery(hashV: Array[Byte]) =
      blocks.filter(_.hash === hashV).exists.run // TODO: check if this comparison really works
    // TODO: is this really the easiest way of querying for the existance of a value?
    // TODO:  check why compiling the query fails:
    // val compiledBlockHashQuery = Compiled(blockHashQuery(_))

    def blockFilter(b: Block) =
      {
      val blockHash = b.getHash.getBytes
 
      (longestChain contains Hash(blockHash)) &&
        ! blockHashQuery(blockHash)
      }

    lazy val filteredBlockStream =
      blockStream filter blockFilter

    
  }
}
