import org.scalatest._
import org.scalatest.prop._

class RawReaderPropertySuite extends PropSpec with Matchers with PropertyChecks {
  /*databaseFile = "blockchain/test.db"
   
  property("populater.end should be the minimum of given end and number of blocks available") 
  {
   forAll (choose(0,280000), minSuccessful(1)) { (n:Int) =>
  	 {
      val populater = new BlocksReader(List(n.toString,"init"))
      populater.start should be(0) 
      populater.end should be (n) // TODO: This is not true. We need the number of blocks available and this is also at least a problem when 0 is given
  	 }
  	 	
  }
  } */
}
