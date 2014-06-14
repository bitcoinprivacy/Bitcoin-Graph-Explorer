package libs

class Hash(val array: Array[Byte]) 
{
	assert(array != null, "Hash(null) ist not defined!")
	
	def deep = array.deep
	override def toString: String = 
	{
	    "X'" + array.map("%02X" format _).mkString +"'"
	}
	
	def equals(that: Hash) =
	{
	  this.deep == that.deep
	}
	
	override def hashCode =
	{	  
	  deep.hashCode	
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
  
