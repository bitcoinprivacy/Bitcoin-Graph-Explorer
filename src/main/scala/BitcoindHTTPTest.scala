/**
 * Created with IntelliJ IDEA.
 * User: stefan
 * Date: 9/11/13
 * Time: 6:37 PM
 * To change this template use File | Settings | File Templates.
 */
import com.sagesex.JsonRPCProxy
import java.sql.DriverManager;
import dispatch._

object BitcoindHTTPTest extends App {

  var startTime = System.currentTimeMillis
  var lastTime = System.currentTimeMillis
  val conn = DriverManager.getConnection("jdbc:mysql://localhost/test?" +
    "user=root&password=12345");


  def getTime: Long = {
    val currentTime = System.currentTimeMillis - lastTime
    lastTime = System.currentTimeMillis
    currentTime
  }
  val rpcuser ="user"
  val rpcpassword ="pass"

  val serverURL = "http://127.0.0.1:8332"
  val proxy = new JsonRPCProxy(serverURL,rpcuser,rpcpassword)
  var splitter = ""
  if (args.length == 1)
  {
    val result = proxy.call(args(0).toString)
    println(result)
  }
  else if (args.length > 1)
  {
    val result = proxy.call(args(0).toString,args(1).toString)
    println(result)
  }
  else
  {
    val blockcountFuture = proxy.call("getblockcount")
    val blockcount = blockcountFuture() match {
      case Right(e) => e.as[Int]
    } // this blocks. not very nice, but OK here, I think

    val stmt = conn.createStatement();
    val rs0 = stmt.executeQuery("SELECT max(block_nr) as value FROM bitcoin.transactions");
    var block_start = 1;
    while (rs0.next()) {
      block_start = rs0.getInt("value");
    }
    val block_end = math.min(blockcount,block_start+10000)
    var insertTableSQL = "INSERT INTO bitcoin.transactions" + " (`block_nr`, `transaction_nr`, `movement_nr`, `mode`, `from`,`to`,`value`) VALUES "
    var elementsCount = 0

    /*val results = */for (i<- block_start to block_end)
    /*yield */{
      getTime
      val hash = proxy.call("getblockhash",i.toString).toString
      val time1 = getTime
      val block = proxy.call("getblock",hash)
      elementsCount += 1
      val arrTransactions =
      block match{
        case a:java.util.HashMap[String,Object] => a.get("tx") match{
            case e:java.util.ArrayList[String] => e
        }
      }
      for (j <- 0 until arrTransactions.size)
      /*yield */{
        val txid = arrTransactions.get(j).toString
        getTime
        val encodedTransaction = proxy.call("getrawtransaction",txid)
        //val time3 = getTime
        val decodedTransaction = proxy.call("decoderawtransaction",encodedTransaction.toString)
        //val time4 = getTime
        decodedTransaction match{
          case a:java.util.HashMap[String,Object] =>
          {
            a.get("vout") match{
              case b:java.util.ArrayList[Object] => {
                for (k <- 0 until b.size)
                  /*yield */b.get(k) match{
                  case c:java.util.HashMap[String,Object] => {
                    insertTableSQL += splitter + "('"+i+"','"+j+"','"+ k +"','out','=>','<=','" + c.get("value") + "')"
                    splitter = ","
                  }
                }
              }
            }
            a.get("vin") match{
              case b:java.util.ArrayList[Object] => {
                for (k <- 0 until b.size)
                  /*yield */b.get(k) match{
                  case c:java.util.HashMap[String,Object] => {
                    insertTableSQL += splitter + "('"+i+"','"+j+"','"+ k +"','in','=>','<=','" + c.get("value") + "')"
                    splitter = ","
                  }
                }
              }
            }
          }
        }
        //print(time4 + time3 + ", ")
      }
      //println()
      println("Block "+ i +" (" + arrTransactions.size + " transactions) in " + time1 + "ms")
    }

    val rs1 = stmt.executeQuery("SELECT count(*) as value FROM bitcoin.transactions");
    var elements = 0
    while (rs1.next()) {
      elements = rs1.getInt("value");
    }

    val preparedStatement = conn.prepareStatement(insertTableSQL)
    preparedStatement.executeUpdate();

    val rs2 = stmt.executeQuery("SELECT count(*) as value FROM bitcoin.transactions");
    var elements_total = 0
    while (rs2.next()) {
      elements_total = rs2.getInt("value");
      elements = elements_total - elements
    }

    println("elements added: " + elements)
    println("total elements: " + elements_total)
  }






}
