package util

import scala.collection._

@SerialVersionUID(100L)
class Hash(val array: mutable.WrappedArray[Byte]) extends AnyVal {
  // this is a convenient, lightweight (value class) abstraction for
  // wrapped Arrays (which already have correct deep equals and hashCode)
  // fast toString/toHex methods adapted from xsbt/Mark Harrah
  // see license terms for these methods below

  override def toString: String =
    {
      val buffer = new StringBuilder(array.length * 2)
      for (i <- 0 until array.length) {
        val b = array(i)
        val bi: Int = if (b < 0) b + 256 else b
        buffer append toHex((bi >>> 4).asInstanceOf[Byte])
        buffer append toHex((bi & 0x0F).asInstanceOf[Byte])
      }
      "X'" + buffer.toString + "'"
    }
  private def toHex(b: Byte): Char =
    {
      require(b >= 0 && b <= 15, "Byte " + b + " was not between 0 and 15")
      if (b < 10)
        ('0'.asInstanceOf[Int] + b).asInstanceOf[Char]
      else
        ('a'.asInstanceOf[Int] + (b - 10)).asInstanceOf[Char]
    }

  def toSomeArray = Some(array.toArray)

 }

object Hash {
  def apply(array: Array[Byte]): Hash = new Hash(array)
  def apply(hex: String): Hash = {
    Hash(
      {
      for { i <- 0 to hex.length - 1 by 2 if i > 0 || !hex.startsWith("0x") }
        yield hex.substring(i, i + 2)
    }.map(Integer.parseInt(_, 16).toByte).toArray
    )
  }

  def zero(length: Int) =
    Hash("0" * 2 * length)

  implicit def hashToArray(a: Hash): Array[Byte] = a.array.toArray

  import Ordering.Implicits.seqDerivedOrdering
  implicit def orderingByWrappedArray: Ordering[Hash] =
        Ordering.by(p => p.array)
}

// license terms for the toHex/toString method:
//Copyright (c) 2008-2014 Typesafe Inc, Mark Harrah, Grzegorz Kossakowski, Josh Suereth, Indrajit Raychaudhuri, Eugene Yokota, and other contributors.
//All rights reserved.
//
//Redistribution and use in source and binary forms, with or without
//modification, are permitted provided that the following conditions
//are met:
//1. Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
//2. Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
//3. The name of the author may not be used to endorse or promote products
//   derived from this software without specific prior written permission.
//
//THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
//IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
//OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
//IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
//INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
//NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
//DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
//THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
//THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
