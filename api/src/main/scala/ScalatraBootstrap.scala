import api._
import org.scalatra._
import javax.servlet.ServletContext
import com.typesafe.config.ConfigFactory

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    lazy val conf = ConfigFactory.load()
    
    context.mount(new MyScalatraServlet, "/*")
    context.initParameters("org.scalatra.environment") = conf.getString("api.mode")
  }
}
