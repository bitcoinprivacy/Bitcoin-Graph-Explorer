package actions

import org.bitcoinj.core._
import core._
import util._

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

trait SlowBlockReader extends BlockReader {
  def useDatabase: Boolean = true

  def saveTransaction(t: Transaction, blockHeight: Int) =
  {
    for (input <- inputsInTransaction(t))
    {
      (saveInput _).tupled(decomposeInput(input))
    }
    var i = 0
    for (output <- outputsInTransaction(t))
    {
      (saveOutput _).tupled((decomposeOutput _).tupled(output, i, blockHeight))
      i += 1
    }
  }

  def saveBlock(b: Hash) = 
    blockDB += (b.array.toArray,longestChain.getOrElse(b,0))

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
      movements += ((spTx.toSomeArray, oTx.toSomeArray, None, Some(oIdx), None,None))
    else {
      q.update(oTx.toSomeArray, Some(oIdx), spTx.toSomeArray)

      // If we move this code after the braces we get the out of order inputs too, but it
      // is not necessary
      val insertedValues = for { o <- movements if o.transaction_hash === arrayByte && o.index === oIdx }
        yield (o.spent_in_transaction_hash,o.transaction_hash,o.address,o.index,o.value, o.block_height)
      val (sp,tx,ad,id,va,bl) = insertedValues.first
      savedMovements = savedMovements.updated((tx.get,id.get), (sp,ad,va,bl))
      println("DEBUG: saved movements = " + savedMovements.size)
    }
  }

  def saveOutput(tx: Hash,adOpt:Option[Array[Byte]],idx:Int,value:Long,height:Int): Unit =
  {
    val x = tx.array.toArray
    val q = for { o <- movements if o.transaction_hash === x && o.index === idx }
      yield (o.address, o.value, o.block_height)
    if (q.length.run == 0)
      movements +=((None, tx.toSomeArray, adOpt, Some(idx), Some(value), Some(height)))
    else
      q.update(adOpt, Some(value), Some(height))
      
    val insertedValues = for { o <- movements if o.transaction_hash === x && o.index === idx }
      yield (o.spent_in_transaction_hash,o.transaction_hash,o.address,o.index,o.value, o.block_height)

    for (a <- insertedValues) {
      val (sp,tx,ad,id,va,bl) = a
      savedMovements = savedMovements.updated((tx.get,id.get), (sp,ad,va,bl))
      println("DEBUG: saved movements = " + savedMovements.size)
    }

  }

  def decomposeInput(i: TransactionInput): (Hash,Int,Hash) = {
    val outpoint = i.getOutpoint
    (Hash(outpoint.getHash.getBytes), outpoint.getIndex.toInt, Hash(i.getParentTransaction.getHash.getBytes))
  }

  def decomposeOutput(o: TransactionOutput, index: Int, blockHeight: Int): (Hash,Option[Array[Byte]],Int,Long,Int) = {
    val addressOption: Option[Array[Byte]] = getAddressFromOutput(o)
    val value = o.getValue.value
    val txHash = Hash(o.getParentTransaction.getHash.getBytes)
    val trans = o.getParentTransaction

    (txHash,addressOption,index,value,blockHeight)
  }
}
