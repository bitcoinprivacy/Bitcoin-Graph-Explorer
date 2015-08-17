package util


import scala.collection.mutable._

import java.nio.ByteBuffer

class UTXOs(val table: Map[Hash,Hash]) extends Map[(Hash,Int),(Hash,Long,Int)]
    with MapLike[(Hash,Int),(Hash,Long,Int),UTXOs] { // we put the address last in the array because it is variable length

  def toTriple(hash: Hash) = {
    val result = hash.array.toArray
    (Hash(result.drop(12)),ByteBuffer.wrap(result.take(8)).getLong,ByteBuffer.wrap(result.slice(8,12)).getInt)
  }

  def toTuple(result: Hash) = {
    val array = result.array.toArray
    (Hash(array.take(32)),ByteBuffer.wrap(array.drop(32)).getInt)
  }

//def +=[B1 >: (util.Hash, Long, Int)](kv: ((util.Hash, Int), B1)): UTXOs = {
  def += (kv: ((util.Hash, Int), (Hash, Long, Int))): UTXOs.this.type = {
    kv match {
      case ((h,i), (a:Hash,v:Long,b:Int)) =>
        table += (toHash(h,i) -> toHash(a,v,b))
        this
    }
  }

  def -=(key: (util.Hash, Int)): UTXOs.this.type = {
    key match {
      case (h,i) =>
        table -=  toHash(h,i)
        this
    }
  }

  def get(key: (util.Hash, Int)): Option[(util.Hash, Long, Int)] = {
    key match {
      case (h,i) =>
        if (table.contains(toHash(h,i))){
          val result = table(toHash(h,i))
          Some(toTriple(result))
        }
        else
          None
    }
  }

  def iterator = table.iterator map {case (h1,h2) => (toTuple(h1),toTriple(h2))}

  override def empty = new UTXOs(Map())

  def toHash(h:Hash,i:Int) = new Hash(h.array ++ toArrayBuf(i))
  def toHash(a:Hash,v:Long,b:Int) = new Hash((toArrayBuf(v,8) ++ toArrayBuf(b) ++ a.array).toArray)

}



