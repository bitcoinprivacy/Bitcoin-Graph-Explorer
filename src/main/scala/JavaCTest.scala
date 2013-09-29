import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
  }
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 9/29/13
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */
object JavaCTest extends App {
  val conn = DriverManager.getConnection("jdbc:mysql://localhost/test?" +
      "user=root&password=12345");
  val stmt = conn.createStatement();

  val rs = stmt.executeQuery("SELECT count(*) as value FROM bitcoin.transactions");
  while (rs.next()) {

    val value = rs.getInt("value");
    println(value);
  }
  // or alternatively, if you don't know ahead of time that
  // the query will be a SELECT...


  //println(stmt.getResultSet().getArray("value"))

}
