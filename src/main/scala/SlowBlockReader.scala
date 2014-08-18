import libs._ // for blocks db and longestChain
import com.google.bitcoin.core._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import scala.collection.JavaConverters._

// extends BlockSource means that it depends on a BlockSource
trait SlowBlockReader extends BlockReader {
 
 def saveInput(oTx: Hash,oIdx:Int,spTx: Hash): Unit =
 { 
   val arrayByte = oTx.array.toArray
   val q = for { o <- movements 
       if o.transaction_hash === arrayByte && o.index === oIdx }
        yield (o.transaction_hash, o.index, o.spent_in_transaction_hash)
   if (q.length.run == 0)
     movements += ((spTx.toSomeArray, oTx.toSomeArray, None, Some(oIdx), None))
   else
     q.update(oTx.toSomeArray, Some(oIdx), spTx.toSomeArray)
   
  }
 
  def saveOutput(tx: Hash,adOpt:Option[Array[Byte]],idx:Int,value:Double): Unit =
  {
    val x = tx.array.toArray
    val q = for { o <- movements 
       if o.transaction_hash === x && o.index === idx }
        yield (o.address, o.value)
    if (q.length.run == 0)
      movements +=((None, tx.toSomeArray, adOpt, Some(idx), Some(value) ))
    else
      q.update(adOpt, Some(value))
  }

  def saveBlock(b: Hash) = { 
    blockDB += (b.array.toArray)
  }

  def pre  = { 
  
  }

  def post = { 
  
  }
} 
