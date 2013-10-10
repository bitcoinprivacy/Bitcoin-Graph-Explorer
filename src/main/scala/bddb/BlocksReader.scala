package bddb

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
import bddb.BlocksReader
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import java.sql.DriverManager;
import play.api.libs.json.JsValue
import scala.concurrent._
import ExecutionContext.Implicits.global
import dispatch._
import com.sagesex.JsonRPCProxy
object BlocksReader {

  var url = "";
  var user = "";
  var pass = "";
  def run = {

    val proxy: JsonRPCProxy = new JsonRPCProxy(this.url,this.user,this.pass);

    val blockCountFuture = proxy.call("getblockcount")
    val blockCount = blockCountFuture() match
    {
      case Right(e) => e.as[Int]
      case _ => 1
    } // this blocks. not very nice, but OK here, I think


    var blockNrs = (1 until blockCount).toSet
    // NOT TO USE SINCE WE ARE NOT YET SAVING ANYTHING
    //val existingBlockNrs =
    //  for(trans <- TransactionsDB)
    //  yield trans.block_nr

    //for (existingBlockNr <- existingBlockNrs)
    //  blockNrs = blockNrs - existingBlockNr

    println("Block nrs. successfully read.")

    for (blockNr <- blockNrs)
    {
      val blockHashFuture = proxy.call("getblockhash",blockNr)
      for (blockHash <- blockHashFuture.right)
      {
        val blockJSONFuture = proxy.call("getblock",blockHash.as[String])
        for ( blockJSON <- blockJSONFuture().right )
        {
          for (transactionHash <-  (blockJSON\"tx").as[Seq[String]])
          {
            println(transactionHash)
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
