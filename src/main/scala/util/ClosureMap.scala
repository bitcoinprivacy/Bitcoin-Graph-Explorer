package util

// wrapper class because LmdbMap only accepts Hash,Hash
import scala.collection.mutable._

import java.nio.ByteBuffer

class ClosureMap(val table: Map[Hash,Hash]) extends Map[Hash,(Int,Hash)]
    with MapLike[Hash,(Int, Hash),ClosureMap] { // we put the address last in the array because it has variable length


  def toTuple(result: Hash) = {
    val array = result.array.toArray
    (ByteBuffer.wrap(array.take(4)).getInt,Hash(array.drop(4)))
  }

//def +=[B1 >: (util.Hash, Long, Int)](kv: ((util.Hash, Int), B1)): UTXOs = {
  def += (kv: (Hash, (Int, Hash))): ClosureMap.this.type = {
    kv match {
      case (s: Hash, (i: Int, a:Hash)) =>
        table += (s -> toHash(i,a))
        this
    }
  }

  def -=(key: Hash): ClosureMap.this.type = {
    table -=  key
    this
  }


  def get(key: Hash): Option[(Int, Hash)] = {
    if (table.contains(key)){
          val result = table(key)
          Some(toTuple(result))
    }
    else
      None
  }

  def iterator = table.iterator map {case (h1,h2) => (h1,toTuple(h2))}

  override def empty = new ClosureMap(Map())

  def toHash(i: Int, h:Hash) = new Hash(toArrayBuf(i).toArray ++ h.array)

}



