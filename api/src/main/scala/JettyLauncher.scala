package api  // remember this package in the sbt project definition

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import com.typesafe.config.ConfigFactory

object JettyLauncher { // this is my entry object as specified in sbt project definition
  def main(args: Array[String]) {
    lazy val conf = ConfigFactory.load()    
    lazy val port = if(System.getenv("PORT") != null) System.getenv("PORT").toInt else conf.getInt("api.port")

    val server = new Server(port)
    val context = new WebAppContext()
    
    

    context setContextPath "/"
    context.setResourceBase("src/main/webapp")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    server.setHandler(context)

    server.start
    server.join

  }
}
