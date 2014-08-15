import libs._ // for blocks db and longestChain
import com.google.bitcoin.core._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.collection.JavaConverters._

// extends BlockSource means that it depends on a BlockSource
trait SlowBlockReader extends BlockSource{
  
  transactionsDBSession {

    val longestChain = getLongestBlockChainHashSet
    def blockHashQuery(hashV: Array[Byte]): Boolean =
      blockDB.filter(_.hash === hashV).exists.run // TODO: check if this comparison really works
    // TODO: is this really the easiest way of querying for the existance of a value?
    // TODO:  check why compiling the query fails:
    // val compiledBlockHashQuery = Compiled(blockHashQuery(_))

    def blockFilter(b: Block) =
      {
      val blockHash = b.getHash.getBytes
 
      (longestChain contains Hash(blockHash)) &&
        ! blockHashQuery(blockHash)
      }

    lazy val filteredBlockSource =
      blockSource withFilter blockFilter
    
    def transactionsInBlock(b: Block) = b.getTransactions.asScala

    def inputsInTransaction(t: Transaction) =
      if (!t.isCoinBase)        t.getInputs.asScala map (Left(_))
      else                          List.empty

    def outputsInTransaction(t: Transaction) = t.getOutputs.asScala map (Right(_))

    def movementsInTransaction(t: Transaction) = inputsInTransaction(t) ++ outputsInTransaction(t)

    lazy val transactionSource:Iterator[Transaction] = filteredBlockSource flatMap transactionsInBlock

    lazy val movementsSource: Iterator[Either[TransactionInput, TransactionOutput]] = transactionSource flatMap movementsInTransaction

   // lazy val (inputStreamSource,outputStreamSource) = (transactionSource map movementsInTransaction).toIterable.unzip

    //lazy val inputSource = inputStreamSource.flatten
    //lazy val outputSource = outputStreamSource.flatten



    var a = 0
    val size = 10000
    var time = System.currentTimeMillis
    for (i <- movementsSource) {
      a = a+1
      if (a==size){
        System.out.println("wir sind geil, " + (System.currentTimeMillis - time)*1000/size + "µs pro movement")
        a = 0
        time = System.currentTimeMillis
      }
    }

  }
}
