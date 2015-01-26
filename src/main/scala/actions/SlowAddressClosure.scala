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
	    for (pair <- mapDSOA) {
	      val (address, dsoa) = pair
	      val foundAddressOption = byAddress(address.array.toArray).firstOption
	      
              val representant = dsoa.find.address
              val foundRepresentantOption = byAddress(representant.array.toArray).firstOption

	      foundAddressOption match {  // A->B is new
                case Some(foundAddress) =>
	         
                  foundRepresentantOption match {
                    case None => // A->C is in db => B->C gets added 
	              dsoa.find.parent = Some(DisjointSetOfAddresses(Hash(foundAddress._2))) 

                    case Some(foundRepresentant) => // A->C, B->D in db => A->C gets updated to A->D
                      val updateQuery = for(p <- addresses if p.hash === foundAddress._1) yield p.representant
                      updateQuery.update(foundRepresentant._2)
                  }
                  mapDSOA remove address

                case None => 
                  foundRepresentantOption match {
                    case Some(foundRepresentant) => // B->C in DB, A->C gets added
                      dsoa.find.parent = Some(DisjointSetOfAddresses(Hash(foundRepresentant._2))) 
                    case None => // nothing previous in db, just add A->B  
	          }
              }
            }
    }

    println("DONE: Tree of size "+ mapDSOA.size + " adapted in %s ms" format (System.currentTimeMillis - timeStart))

    mapDSOA
  }

  def generateTree: HashMap[Hash, DisjointSetOfAddresses] = {
    val timeStart = System.currentTimeMillis
    println("DEBUG: Generating tree ...")
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
    println("DONE: Tree generated in %s ms" format (System.currentTimeMillis - timeStart))
    tree
  }

}
