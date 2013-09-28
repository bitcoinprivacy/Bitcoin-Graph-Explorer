/**
 * Created with IntelliJ IDEA.
 * User: stefan
 * Date: 9/11/13
 * Time: 6:37 PM
 * To change this template use File | Settings | File Templates.
 */

/*
import sys.process.stringSeqToProcess



object BitcoindConsoleTest extends App {

  var time = System.currentTimeMillis
  var time2:Long = 0

  val rpcuser ="user"
  val rpcpassword ="pass"



val blockcount = (Seq("bitcoind","-rpcuser=user","-rpcpassword=pass","getblockcount")!!).trim.toInt
var elementsCount = 0



  for (i<- 1 to blockcount)
  {
    time2 = System.currentTimeMillis - time
    time = System.currentTimeMillis
    val hash = Seq("bitcoind","-rpcuser=user","-rpcpassword=pass","getblockhash",i.toString)!!

    val block = Seq("bitcoind","-rpcuser=user","-rpcpassword=pass","getblock",hash)!!



    /*val arrTransactions =
      block match{
        case a:java.util.HashMap[String,Object] => a.get("tx") match{
          case e:java.util.ArrayList[String] => e
        }
      }*/
    val time3 = time2
    time2 = System.currentTimeMillis - time
    time = System.currentTimeMillis
    println("Block " + i + " ( ? transactions ) in " + time3 + " / " + time2)


    /*for (j <- 0 until arrTransactions.size())
    {
      elementsCount += 1
      val txid = arrTransactions.get(j).toString
      val encodedTransaction = proxy.call("getrawtransaction",txid)
      val decodedTransaction = proxy.call("decoderawtransaction",encodedTransaction)
      /*println(*/decodedTransaction match{
        case a:java.util.HashMap[String,Object] =>
        {
          a.get("vout") match{
            case b:java.util.ArrayList[Object] => {
              for (k <- 0 until b.size())
                  yield b.get(k) match{
                    case c:java.util.HashMap[String,Object] => {
                      //c.get("value")
                    }
                  }
            }
          }
        }
      })
    } */
  }
  println("elements analyzed: "+elementsCount)




}  */
