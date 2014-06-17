package libs

class Hash(val array: Array[Byte]) 
{
	assert(array != null, "Hash(null) ist not defined!")

	override def toString: String = 
	{
	    "X'" + array.map("%02X" format _).mkString +"'"
	}

	override def equals(that: Any): Boolean =
	that match {
	  case t:Hash =>
      array.deep == t.array.deep
    case _ =>
	    false
	}
	
	override def hashCode =
	{
    java.util.Arrays.hashCode(array.asInstanceOf[Array[Byte]])
	}
	

}

object Hash   
{
  def apply(array: Array[Byte]) = new Hash(array)
  def apply(hex: String) =	{
	  new Hash (	  
	      {
	        for {i <- 0 to hex.length - 1 by 2 if i > 0 || !hex.startsWith("0x")} 
	          yield hex.substring(i, i + 2)
	      }.map(Integer.parseInt(_, 16).toByte).toArray
      )
    }
  
  def zero(length: Int) =
	  Hash("0"*2*length)
}
  
