/**
 * Created by yzark on 12/16/13.
 */
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
package object libs {
  def databaseSession(f: => Unit): Unit = {
    Database.forURL(
      url = "jdbc:mysql://localhost/bitcoin",
      driver = "com.mysql.jdbc.Driver",
      user = "root",
      password = "12345"

    ) withSession f
  }
}
