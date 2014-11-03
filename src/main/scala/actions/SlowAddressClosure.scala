import core._
import util._
import java.io._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession
import util.DisjointSetOfAddresses

class SlowAddressClosure (args:List[String]) extends AddressClosure (args)
{
  override def adaptTreeIfNecessary(mapDSOA: HashMap[Hash, DisjointSetOfAddresses]): HashMap[Hash, DisjointSetOfAddresses] =
  {
    val timeStart = System.currentTimeMillis
    println("     Adapting tree to database ...")

    transactionDBSession {
	    val byAddress = addresses.findBy( t => t.hash)
	    for (pair <- mapDSOA){
	      val (address, dsoa) = pair
	      val found = byAddress(address.array.toArray).firstOption
	      
	      for (query <- found) {
	        
	        dsoa.find.parent = Some(DisjointSetOfAddresses(Hash(query._2)))
	        mapDSOA remove address
	      }
	    }
    
    }

    println("     Tree of size "+ mapDSOA.size + " adapted in %s ms" format (System.currentTimeMillis - timeStart))

    mapDSOA
  }
}
