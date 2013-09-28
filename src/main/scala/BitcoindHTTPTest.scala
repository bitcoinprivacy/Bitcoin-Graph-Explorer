/**
 * Created with IntelliJ IDEA.
 * User: stefan
 * Date: 9/11/13
 * Time: 6:37 PM
 * To change this template use File | Settings | File Templates.
 */
import com.googlecode.jj1.ServiceProxy
import java.net._
object BitcoindHTTPTest extends App {

  var startTime = System.currentTimeMillis
  var lastTime = System.currentTimeMillis

  def getTime: Long = {
    val currentTime = System.currentTimeMillis - lastTime
    lastTime = System.currentTimeMillis
    currentTime
  }
  val rpcuser ="user"
  val rpcpassword ="pass"
  Authenticator.setDefault(new Authenticator{
    override def getPasswordAuthentication:PasswordAuthentication =
      new PasswordAuthentication (rpcuser, rpcpassword.toCharArray)
    })

  val serverURL = "http://user:pass@127.0.0.1:8332"
  val proxy = new ServiceProxy(serverURL)

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
    //val blockcount = proxy.call("getblockcount").toString.toInt
    val blockcount = 100000
    var elementsCount = 0
    val results = for (i<- 1 to blockcount)
    yield {
      getTime
      val hash = proxy.call("getblockhash",i:java.lang.Integer).toString
      val time1 = getTime
      val block = proxy.call("getblock",hash);
      val time2 = getTime
      elementsCount += 1
      val arrTransactions =
      block match{
        case a:java.util.HashMap[String,Object] => a.get("tx") match{
            case e:java.util.ArrayList[String] => e
        }
      }


      for (j <- 0 until arrTransactions.size)
      yield {
        val txid = arrTransactions.get(j).toString
        getTime
        val encodedTransaction = proxy.call("getrawtransaction",txid)
        val time3 = getTime
        val decodedTransaction = proxy.call("decoderawtransaction",encodedTransaction)
        val time4 = getTime
        decodedTransaction match{
          case a:java.util.HashMap[String,Object] =>
          {
            a.get("vout") match{
              case b:java.util.ArrayList[Object] => {
                for (k <- 0 until b.size)
                  yield b.get(k) match{
                  case c:java.util.HashMap[String,Object] => {
                    c.get("value")
                  }
                }
              }
            }
            a.get("vin") match{
              case b:java.util.ArrayList[Object] => {
                for (k <- 0 until b.size)
                  yield b.get(k) match{
                  case c:java.util.HashMap[String,Object] => {
                    c.get("value")
                  }
                }
              }
            }
          }
        }
        print(time4 + time3 + ", ")
      }
      println()
      println("Block "+ i +" (" + arrTransactions.size + " transactions) in " +  time2 + time1 )
    }

    println("elements analyzed: " + elementsCount)
  }






}
