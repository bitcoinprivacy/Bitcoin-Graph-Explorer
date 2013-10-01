import java.io.{InputStreamReader, BufferedReader, InputStream, OutputStream}
import java.lang.{StringBuilder, Object, String}
import java.net.{URL, URLConnection}
import scala.Predef.String
import scala.StringBuilder

/**
 * Created with IntelliJ IDEA.
 * User: stefan
 * Date: 9/30/13
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
class JsonRPCProxy(val url: String) {

}

class

ServiceProxy {
  private var gid: Int = 100
  private var url: String = null
  private var name: String = null

  def this(url: String, name: String) {
    this()
    this.name = name
    this.url = url
  }

  def this(url: String) {
    this()
    `this`(url, null)
  }

  def get(property: String): ServiceProxy = {
    return new ServiceProxy(url, if (name == null) property else name + "." + property)
  }

  def call(mName: String, parameters: AnyRef*): AnyRef = {
    return get(mName).execute(parameters)
  }

  def execute(parameters: AnyRef*): AnyRef = {
    try {
      val values: Map[String, AnyRef] = new HashMap[String, AnyRef]
      values.put("method", name)
      values.put("params", parameters)
      values.put("id", "" + ({
        gid += 1; gid - 1
      }))
      var content: String = null

      try {
        val writer: Nothing = new Nothing(new Nothing)
        content = writer.write(values)
      }
      catch {
        case e: NullPointerException => {
          throw new Nothing("cannot encode object to json", e)
        }
      }
      val connection: URLConnection = new URL(url).openConnection
      connection.setRequestProperty("method", "POST")
      connection.setUseCaches(false)
      connection.setDoInput(true)
      connection.setDoOutput(true)
      val out: OutputStream = connection.getOutputStream
      out.write(content.getBytes("utf-8"))
      out.close
      connection.connect
      val in: InputStream = connection.getInputStream
      val i: BufferedReader = new BufferedReader(new InputStreamReader(in, "utf-8"))
      val sb: StringBuilder = new StringBuilder
      var line: String = null
      while ((({
        line = i.readLine; line
      })) != null) {
        sb.append(line)
        sb.append("\n")
      }
      in.close
      var result: Map[String, AnyRef] = null
      try {
        val reader: Nothing = new Nothing(new Nothing)
        result = reader.read(sb.toString).asInstanceOf[Map[String, AnyRef]]
      }
      catch {
        case e: Exception => {
          throw new Nothing("cannot decode json", e)
        }
      }
      if (result.get("error") != null) {
        throw new Nothing(result.get("error").toString)
      }
      return result.get("result")
    }
    catch {
      case e: Nothing => {
        throw e
      }
      case e: Exception => {
        e.printStackTrace
        throw new Nothing("shit")
      }
    }
  }

