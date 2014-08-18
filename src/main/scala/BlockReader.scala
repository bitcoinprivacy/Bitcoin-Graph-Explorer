import libs._ // for blocks db and longestChain
import com.google.bitcoin.core._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.collection.JavaConverters._

// extends BlockSource means that it depends on a BlockSource
trait BlockReader extends BlockSource {

  def saveInput(oTx: Hash,oIdx:Int,spTx: Hash): Unit
  def saveOutput(tx: Hash,adOpt:Option[Array[Byte]],idx:Int,value:Double): Unit
  def saveBlock(b: Hash): Unit
  def pre: Unit
  def post: Unit
  
  pre
  var savedBlockSet: Set[Hash] = Set.empty    
  val longestChain = getLongestBlockChainHashSet

  transactionsDBSession {    

    val savedBlocks = for (b <- blockDB) yield (b.hash)
    for (c <- savedBlocks) savedBlockSet = savedBlockSet + Hash(c)
    
    var a = 0
    val time = System.currentTimeMillis
    
    for {movement <- movementsSource} 
    {
      a +=1
      if (a%1000==0) 
        System.out.println(a + ": "+(System.currentTimeMillis - time)*1000/a+" µs")
      decomposeAndSave(movement)     
    }
    
    post  
  }

  
  
  def blockFilter(b: Block) =
  {
    val blockHash = Hash(b.getHash.getBytes)
    (longestChain contains blockHash) && 
      !(savedBlockSet contains blockHash)
  }

    lazy val filteredBlockSource =
      blockSource withFilter blockFilter
    
    def transactionsInBlock(b: Block) = b.getTransactions.asScala

    def inputsInTransaction(t: Transaction) =
      if (!t.isCoinBase)        t.getInputs.asScala map (Left(_))
      else                          List.empty

    def outputsInTransaction(t: Transaction) = { 
      val outputs = t.getOutputs.asScala 
      outputs zip (0 until outputs.length) map (Right(_))
    }

    def movementsInTransaction(t: Transaction) = inputsInTransaction(t) ++ outputsInTransaction(t)

    lazy val transactionSource:Iterator[Transaction] = filteredBlockSource flatMap {b => saveBlock(Hash(b.getHash.getBytes)) ; transactionsInBlock(b)}

    lazy val movementsSource: Iterator[Either[TransactionInput, (TransactionOutput,Int)]] = transactionSource flatMap movementsInTransaction
    
  def decomposeAndSave(movement: Either[TransactionInput, (TransactionOutput,Int)]): Unit = movement match { 
        case Left(i) => (saveInput _).tupled(decomposeInput(i)) 
        case Right(i) => (saveOutput _).tupled((decomposeOutput _).tupled(i))
      }  

  

  def decomposeInput(i: TransactionInput): (Hash,Int,Hash) = {
    val outpoint = i.getOutpoint
    (Hash(outpoint.getHash.getBytes), outpoint.getIndex.toInt, Hash(i.getParentTransaction.getHash.getBytes))
  }

  def decomposeOutput(o: TransactionOutput, index: Int): (Hash,Option[Array[Byte]],Int,Double) = { 
    val addressOption: Option[Array[Byte]] = getAddressFromOutput(o) 
    val value = o.getValue.doubleValue
    val txHash = Hash(o.getParentTransaction.getHash.getBytes)
    (txHash,addressOption,index,value)
  }

  def getAddressFromOutput(output: TransactionOutput): Option[Array[Byte]] =
    try
    {
      Some(output.getScriptPubKey.getToAddress(params).getHash160)
    } 
    catch 
    {
      case e: ScriptException =>
        try 
        {
          val script = output.getScriptPubKey.toString
          //TODO: 
          // can we generate an address for pay-to-ip?

          if (script.startsWith("[65]")) {
            val pubkeystring = script.substring(4, 134)
            import Utils._
            val pubkey = Hash(pubkeystring).array.toArray
            val address = new Address(params, sha256hash160(pubkey))
            Some(address.getHash160)
          } else { // special case because bitcoinJ doesn't support pay-to-IP scripts
            None
          }
        }
        catch {
          case e: ScriptException =>
            println("bad transaction output: " + output.getParentTransaction.getHash)
            None
        }
      
    }
  

}
