/**
 * Created with IntelliJ IDEA.
 * User: stefan 
 * Date: 10/29/13
 * Time: 9:58 PM
 * To change this template use File | Settings | File Templates.
 */
import scalax.io._

object RawBlockFileReader extends App {
  
  val Source = Resource.fromFile("/home/stefan/.bitcoin/blocks/blk00000.dat")

  def splitIntoBlockMessages (bytes: LongTraversable[Byte]): 
      Stream[Array[Byte]] = {
    if (!bytes.bytesAsInts.startsWith(Seq(0xf9,0xbe,0xb4,0xd9)))
      throw new Exception("Unknown File Format")
    else
    { import java.nio.ByteBuffer
      val count = ByteBuffer.wrap(bytes.slice(4,8).toArray).getLong
      val (hh,tt) = bytes.splitAt(count+8)
      hh.toArray #:: splitIntoBlockMessages(tt)
    }
    
  }
  
  println(splitIntoBlockMessages(Source))
}
 

   










