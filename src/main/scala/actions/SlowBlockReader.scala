package actions

import org.bitcoinj.core._
import core._
import util._

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

trait SlowBlockReader extends BlockReader {
  def useDatabase: Boolean = true

  def saveTransaction(t: Transaction) =
  {
    for (input <- inputsInTransaction(t))
    {
      (saveInput _).tupled(decomposeInput(input))
    }
    var i = 0
    for (output <- outputsInTransaction(t))
    {
      (saveOutput _).tupled((decomposeOutput _).tupled(output, i))
      i += 1
    }
  }

  def saveBlock(b: Hash) = { 
    blockDB += (b.array.toArray)
  }

  def pre  = { 
  
  }

  def post = { 
  
  }

  def saveInput(oTx: Hash,oIdx:Int,spTx: Hash): Unit =
  {
    val arrayByte = oTx.array.toArray

    val q = for { o <- movements if o.transaction_hash === arrayByte && o.index === oIdx }
      yield (o.transaction_hash, o.index, o.spent_in_transaction_hash)

    if (q.length.run == 0)
      movements += ((spTx.toSomeArray, oTx.toSomeArray, None, Some(oIdx), None))
    else
      q.update(oTx.toSomeArray, Some(oIdx), spTx.toSomeArray)
  }

  def saveOutput(tx: Hash,adOpt:Option[Array[Byte]],idx:Int,value:Long): Unit =
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

  def decomposeInput(i: TransactionInput): (Hash,Int,Hash) = {
    val outpoint = i.getOutpoint
    (Hash(outpoint.getHash.getBytes), outpoint.getIndex.toInt, Hash(i.getParentTransaction.getHash.getBytes))
  }

  def decomposeOutput(o: TransactionOutput, index: Int): (Hash,Option[Array[Byte]],Int,Long) = {
    val addressOption: Option[Array[Byte]] = getAddressFromOutput(o)
    val value = o.getValue.value
    val txHash = Hash(o.getParentTransaction.getHash.getBytes)
    (txHash,addressOption,index,value)
  }
}
