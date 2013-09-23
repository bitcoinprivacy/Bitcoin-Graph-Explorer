/**
 * Created with IntelliJ IDEA.
 * User: stefan
 * Date: 9/11/13
 * Time: 6:37 PM
 * To change this template use File | Settings | File Templates.
 */


import com.googlecode.jj1.ServiceProxy
import java.net._



object BitcoindTest extends App {
  val rpcuser ="user"
  val rpcpassword ="pass"

  Authenticator.setDefault(new Authenticator(){
    override def getPasswordAuthentication():PasswordAuthentication =
      new PasswordAuthentication (rpcuser, rpcpassword.toCharArray())
    })

  val serverURL = "http://user:pass@127.0.0.1:8332"
  val proxy = new ServiceProxy(serverURL)

  if (args.length == 1)
  {
    val result = proxy.call(args(0))
    println(result)
  }
  else if (args.length > 1)
  {
    val result = proxy.call(args(0),args(1))
    println(result)
  }
  else
  {
    println("missing parameter")
  }




}
