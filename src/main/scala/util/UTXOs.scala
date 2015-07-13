package util


import scala.collection._

import java.nio.ByteBuffer

class UTXOs(val table: immutable.HashMap[Hash,Hash]) extends immutable.Map[(Hash,Int),(Hash,Long,Int)]
    with immutable.MapLike[(Hash,Int),(Hash,Long,Int),UTXOs] { // we put the address last in the array because it is variable length

  def toTriple(hash: Hash) = {
    val result = hash.array.toArray
    (Hash(result.drop(12)),ByteBuffer.wrap(result.take(8)).getLong,ByteBuffer.wrap(result.slice(8,12)).getInt)
  }

  def toTuple(result: Hash) = {
    val array = result.array.toArray
    (Hash(array.take(32)),ByteBuffer.wrap(array.drop(32)).getInt)
  }
    
  def +[B1 >: (util.Hash, Long, Int)](kv: ((util.Hash, Int), B1)): UTXOs = {
    kv match {
      case ((h,i), (a:Hash,v:Long,b:Int)) =>
        new UTXOs(table.updated(toHash(h,i),toHash(a,v,b)))
    }
  }

  def -(key: (util.Hash, Int)): util.UTXOs = {
    key match {
      case (h,i) =>
        new UTXOs(table - toHash(h,i))
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

  override def empty = new UTXOs(immutable.HashMap())

  def toArrayBuf[A:IntOrLong](x:A, INTBYTES:Int = 4)(implicit f:IntOrLong[A]): mutable.ArrayBuffer[Byte] = {
    val buf = new mutable.ArrayBuffer[Byte](f.length)
    for(i <- 0 until f.length) {
      buf += f.&(f.>>>(x,(f.length - i - 1 << 3)), 0xFF).toByte
    }
    buf
  }

  trait IntOrLong[A]{
    def length: Int
    def >>> : (A,Long) => A
    def & : (A, Int) => Long
  }

  object IntOrLong {
    implicit object IntWitness extends IntOrLong[Int]{
      def length=4
      def >>> = (_ >>> _)
      def & = (_ & _)
    }

    implicit object LongWitness extends IntOrLong[Long]{
      def length=8
      def >>> = (_ >>> _)
      def & = (_ & _)
    }
  }

  def toHash(h:Hash,i:Int) = new Hash(h.array ++ toArrayBuf(i))
  def toHash(a:Hash,v:Long,b:Int) = new Hash((toArrayBuf(v,8) ++ toArrayBuf(b) ++ a.array).toArray)

}
  
  

