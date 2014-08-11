import libs._ // for blocks db and longestChain
import com.google.bitcoin.core._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.collection.JavaConverters._

trait SlowBlockReader extends BlockSource{

  
  transactionsDBSession {

    val longestChain = getLongestBlockChainHashSet
    def blockHashQuery(hashV: Array[Byte]): Boolean =
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
    
    def transactionsInBlock(b: Block) = b.getTransactions.asScala.toStream

    def inputsInTransaction(t: Transaction) = 
      if (!t.isCoinBase)        t.getInputs.asScala.toStream
      else                          Stream.empty
      
    def outputsInTransaction(t: Transaction) = t.getOutputs.asScala.toStream

    lazy val transactionStream = filteredBlockStream flatMap transactionsInBlock

    lazy val inputStream = transactionStream flatMap inputsInTransaction

    lazy val outputStream = transactionStream flatMap outputsInTransaction
      
    System.out.println("HOLA");

    for (i <- inputStream.take(50))   System.out.println(i.toString)
    
    for (o <- outputStream.take(50))  System.out.println(o.toString)


  }
}
