import core._ 
import util._
import java.io._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

class FastAddressClosure(args:List[String]) extends AddressClosure(args) { 
// we will use later for updates
  def adaptTreeIfNecessary(mapDSOA: HashMap[Hash, DisjointSetOfAddresses]): HashMap[Hash, DisjointSetOfAddresses] =
  {
    mapDSOA
  }

  def initializeAddressDatabaseFileIfNecessary = { 
    new File(addressesDatabaseFile).delete
}

  def createTablesIfNecessary = addressesDBSession
  {
    addresses.ddl.create
  }
}
