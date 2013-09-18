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

  val serverURL = "http://127.0.0.1:8332"
  val proxy = new ServiceProxy(serverURL)
  val result = proxy.call("getinfo")
  println(result)




}
