import core._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

import util._

class SlowAddressClosure(savedMovements: Map[(Hash,Int),(Option[Array[Byte]],Option[Array[Byte]],Option[Long],Option[Int])]) extends AddressClosure
{
  override def adaptTreeIfNecessary(mapDSOA: HashMap[Hash, DisjointSetOfAddresses]): HashMap[Hash, DisjointSetOfAddresses] =
  {
    val timeStart = System.currentTimeMillis
    println("DEBUG: Adapting tree to database ...")

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

    println("DONE: Tree of size "+ mapDSOA.size + " adapted in %s ms" format (System.currentTimeMillis - timeStart))

    mapDSOA
  }

  def generateTree: HashMap[Hash, DisjointSetOfAddresses] = {

    val mapAddresses:HashMap[Hash, Array[Hash]] = HashMap.empty
    var tree: HashMap[Hash, DisjointSetOfAddresses] = HashMap.empty

    for {(p,q) <- savedMovements
      spent <- q._1
      address <- q._2
    }
    {
      val list: Array[Hash] = mapAddresses.getOrElse(Hash(spent), Array())
      mapAddresses.update(Hash(spent), list :+ Hash(address))
    }

    insertValuesIntoTree(mapAddresses, tree)

    tree
  }

}
