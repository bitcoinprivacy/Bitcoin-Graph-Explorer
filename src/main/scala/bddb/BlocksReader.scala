package bddb

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import java.sql.DriverManager;
import play.api.libs._
import json.JsValue
import scala.concurrent._
import ExecutionContext.Implicits.global
import dispatch._
import com.sagesex.JsonRPCProxy
import scala.concurrent.duration._


class BlocksReader(val url:String, user:String, pass:String, timeout:Int, maxcalls:Int) {


  def run = {

    val proxy: JsonRPCProxy = new JsonRPCProxy(url,user,pass, timeout, maxcalls);

    val blockCountFuture = proxy.call("getblockcount")
    //val blockCount = for (nr <- blockCountFuture) yield nr
    val blockCount = 100000

    var blockNrs = (1 until blockCount).toSet
    // NOT TO USE SINCE WE ARE NOT YET SAVING ANYTHING
    //val existingBlockNrs =
    //  for(trans <- TransactionsDB)
    //  yield trans.block_nr

    //for (existingBlockNr <- existingBlockNrs)
    //  blockNrs = blockNrs - existingBlockNr
    
    println("Block nrs. successfully read.")

    var step = 0;
    var count = 0;
    for (blockNr <- blockNrs)
    {
      val blockHashFuture = proxy.call("getblockhash",blockNr)
      blockHashFuture onFailure {case(t) => println ("Failure :" + t.getMessage) }
      for (blockHash <- blockHashFuture)
      {
        val blockJSONFuture = proxy.call("getblock",blockHash.as[String])
        blockJSONFuture onFailure {case(t) => println ("Failure :" + t.getMessage) }
        for ( blockJSON <- blockJSONFuture )
        {
          count+=1
          println("=>"+count)
          for (transactionHash <-  (blockJSON\"tx").as[Seq[String]])
          {
            //println(transactionHash)
            // call getrawtransaction
            // call decoderawtransaction
            // read "vout" or "vin" to get "value"
            // write (i,j,k,out/in,=>,<=,value)
          }
        }
      }
    }
  }
}
