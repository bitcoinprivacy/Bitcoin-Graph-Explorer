package bddb

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 10/10/13
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
import com.sagesex.JsonRPCProxy
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

class BlocksReader(val url:String, user:String, pass:String, timeout:Int, maxcalls:Int) {


  def run = {

    val proxy: JsonRPCProxy = new JsonRPCProxy(url,user,pass, timeout, maxcalls);

    val blockCount = proxy.call("getblockcount").as[Int]

    var blockNrs = (1 until blockCount).toSet
    val existingBlockNrs =
      for(trans <- TransactionsDB)
        yield trans.block_nr

    for (existingBlockNr <- existingBlockNrs)
      blockNrs = blockNrs - existingBlockNr
    
    println("Block nrs. successfully read: "+blockCount)

    for (blockNr <- blockNrs)
    {
      val blockHash = proxy.call("getblockhash",blockNr)
      val blockJSON = proxy.call("getblock",blockHash.as[String])
      for (transactionHash <-  (blockJSON\"tx").as[Seq[String]])
      {
        val rawTransaction = proxy.call("getrawtransaction",transactionHash).as[String]
        val transaction = proxy.call("decoderawtransaction",rawTransaction)
        //TransactionsDB.insert(blockNr,1,2,"out","out","out","out")

      }
      println(blockNr + "X")

    }
  }
}
