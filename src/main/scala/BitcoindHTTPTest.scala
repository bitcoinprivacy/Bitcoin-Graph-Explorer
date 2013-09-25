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

  //var time = System.currentTimeMillis
  //var time2:Long = 0

  val rpcuser ="user"
  val rpcpassword ="pass"
  def hex2Bytes(hex: String): Array[Byte] = {
    (for {i <- 0 to hex.length - 1 by 2 if i > 0 || !hex.startsWith("0x")}
    yield hex.substring(i, i + 2))
      .map(Integer.parseInt(_, 16).toByte).toArray
  }
  Authenticator.setDefault(new Authenticator(){
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

    val blockcount = proxy.call("getblockcount").toString.toInt
    var elementsCount = 0



    for (i<- 1 to blockcount)
    {
      //time2 = System.currentTimeMillis - time
      //time = System.currentTimeMillis
      val hash = proxy.call("getblockhash",i:java.lang.Integer).toString
      val block = proxy.call("getblock",hash);


      val arrTransactions =
      block match{
        case a:java.util.HashMap[String,Object] => a.get("tx") match{
            case e:java.util.ArrayList[String] => e
        }
      }
      //val time3 = time2
      //time2 = System.currentTimeMillis - time
      //time = System.currentTimeMillis
      //println("Block "+ i +" (" + arrTransactions.size() + " transactions) in " + time3 + " / " + time2)
      println("Block "+i)

      /*for (j <- 0 until arrTransactions.size())
      {
        elementsCount += 1
        println(elementsCount)
        val txid = arrTransactions.get(j).toString
        val encodedTransaction = proxy.call("getrawtransaction",txid)
        //time2 = System.currentTimeMillis()
        val decodedTransaction = proxy.call("decoderawtransaction",encodedTransaction)
        //println(System.currentTimeMillis()-time2)
        decodedTransaction match{
          case a:java.util.HashMap[String,Object] =>
          {
            a.get("vout") match{
              case b:java.util.ArrayList[Object] => {
                for (k <- 0 until b.size())
                    b.get(k) match{
                      case c:java.util.HashMap[String,Object] => {
                        c.get("value")
                      }
                    }
              }
            }
          }
        }
      }*/
    }
    println("elements analyzed: "+elementsCount)
  }




}
