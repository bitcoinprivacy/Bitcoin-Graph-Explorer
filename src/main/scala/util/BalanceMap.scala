package util

// wrapper class because LmdbMap only accepts Hash,Hash
import scala.collection.mutable._

import java.nio.ByteBuffer

class BalanceMap(val table: Map[Hash,Hash]) extends Map[Hash,Long]
    with MapLike[Hash,Long,BalanceMap] {


  def toLong(result: Hash) = {
    val array = result.array.toArray
    ByteBuffer.wrap(array).getLong
  }

//def +=[B1 >: (util.Hash, Long, Int)](kv: ((util.Hash, Int), B1)): UTXOs = {
  def += (kv: (Hash, Long)): BalanceMap.this.type = {
    kv match {
      case (s: Hash, i:Long) =>
        table += (s -> toHash(i))
        this
    }
  }

  def -=(key: Hash): BalanceMap.this.type = {
    table -=  key
    this
  }


  def get(key: Hash): Option[Long] = {
    if (table.contains(key)){
          val result = table(key)
          Some(toLong(result))
    }
    else
      None
  }

  def iterator = table.iterator map {case (h1,h2) => (h1,toLong(h2))}

  override def empty = new BalanceMap(Map())

  def toHash(i: Long) = new Hash(toArrayBuf(i).toArray)

  override def values = table.values.map(toLong(_))
}



