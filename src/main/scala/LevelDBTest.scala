/**
 * Created with IntelliJ IDEA.
 * User: stefan
 * Date: 8/21/13
 * Time: 6:24 PM
 * To change this template use File | Settings | File Templates.
 */
import org.iq80.leveldb._
import org.iq80.leveldb.impl.Iq80DBFactory._;
import java.io._;

object LevelDBTest {

  var options = new Options();
  options.createIfMissing(true);
  var db = factory.open(new File("example"), options);
  try {

    // Use the db in here....
    db.put(bytes("Tampa"), bytes("rocks"))
    var value = asString(db.get(bytes("Tampa")))
    printf("wir sind gut!")
    db.delete(bytes("Tampa"))

  } finally {
    // Make sure you close the db to shutdown the
    // database and avoid resource leaks.
  db.close();


  }
}
