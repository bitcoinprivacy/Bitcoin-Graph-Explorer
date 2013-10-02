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

  var db = factory.open(new File("/home/stefan/.bitcoin/blocks/index"), options);
  try {

    val iterator = db.iterator();
    try {
      iterator.seekToFirst()
      while (iterator.hasNext()) {

        val key = asString(iterator.peekNext().getKey());
        val value = asString(iterator.peekNext().getValue());
        System.out.println(key+" = "+value);
        iterator.next()
      }
    } finally {
      // Make sure you close the iterator to avoid resource leaks.
      iterator.close()
    }

  } finally {
    // Make sure you close the db to shutdown the
    // database and avoid resource leaks.
  db.close();


  }
}
