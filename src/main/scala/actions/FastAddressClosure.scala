import core._ 
import util._
import java.io._
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable.HashMap
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

class FastAddressClosure(args:List[String]) extends AddressClosure(args)
{

  override def initializeAddressDatabaseFileIfNecessary =
  {
    new File(addressesDatabaseFile).delete
  }

  override def createTablesIfNecessary = addressDBSession
  {
    addresses.ddl.create
  }

  override def createIndexesIfNecessary = addressDBSession
  {
    var clockIndex = System.currentTimeMillis
    println("Creating indexes ...")
    (Q.u + "create index if not exists representant on addresses (representant)").execute
    (Q.u + "create unique index if not exists hash on addresses (hash)").execute
    println("=============================================")
    println("")
    clockIndex = System.currentTimeMillis - clockIndex
    println("/////////////////////////////////////////////")
    println("Indices created in %s s" format (clockIndex / 1000))
  }
}
