import dispatch._
import java.net._
/**
 * Created with IntelliJ IDEA.
 * User: stefan
 * Date: 9/11/13
 * Time: 6:37 PM
 * To change this template use File | Settings | File Templates.
 */
object BitcoindTest {
  val rpcuser ="user"
  val rpcpassword ="pass"

  Authenticator.setDefault(new Authenticator(){
    override def getPasswordAuthentication():PasswordAuthentication =
      new PasswordAuthentication (rpcuser, rpcpassword.toCharArray())
    })

  val svc = url("http://api.hostip.info/country.php")
  val country = Http(svc OK as.String)
}
