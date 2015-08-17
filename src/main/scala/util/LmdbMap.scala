package util

import scala.collection.convert.WrapAsScala._
import org.fusesource.lmdbjni._
import org.fusesource.lmdbjni.Constants._
import scala.collection.mutable.Map

object LmdbMap
{
  def empty: LmdbMap = new LmdbMap

  def create(name: String) = {
    val dir = new java.io.File(conf.getString("mdb.path") + "/" + name)
    recreateDir(dir)
    open(name)
  }

  def open(name: String) = new LmdbMap(name)

  def recreateDir(dir: java.io.File): Unit  = {
    if (dir.isDirectory()) {
      val files = dir.listFiles();
      if (files != null)
        for (file <- files)
          if (file.isDirectory()) {
            recreateDir(file);
          } else {
            file.delete();
          }
    }
    dir.delete();
        dir.mkdirs();

  }

  }

class LmdbMap(val name: String = java.util.UUID.randomUUID.toString)
    extends collection.mutable.Map[Hash,Hash] with collection.mutable.MapLike[Hash,Hash, LmdbMap]{

  var currentTransactionSize = 0

  // File dir = new File("/tmp/mdb");
  // //setLmdbLibraryPath();
  // valueVal = new JNI.MDB_val();
  // keyVal = new JNI.MDB_val();
  // recreateDir(dir);
 // var env = new Env();
//  env.open(dir.getAbsolutePath());
//  env.setMapSize(4_294_967_296L);
//  database = env.openDatabase("test");


  var env = new Env()
  env.open(conf.getString("mdb.path") + "/" + name, NOSYNC)
  env.setMapSize(1024 * 1024 * 1024 * 1024L) // 1TB
  var db = env.openDatabase("test")
  //var tx = env.createWriteTransaction
  val cache: Map[Hash,Hash] = Map.empty
  val transactionSize = conf.getInt("lmbdTransactionSize")

  override def empty: LmdbMap = new LmdbMap

  def -=(key: Hash): LmdbMap.this.type = {
    if (cache.contains(key))
      cache -= key
    else
      db.delete(key.array.toArray)

    this
  }

  def +=(kv: (Hash, Hash)): LmdbMap.this.type = {

    cache += kv

    if (cache.size == transactionSize)
    {
      val t = System.currentTimeMillis
      val tx = env.createWriteTransaction
       for (kv <- cache)
         db.put(tx, kv._1.array.toArray, kv._2.array.toArray)
      tx.commit
      println("commit took " + (System.currentTimeMillis - t) + " ms")
      cache.clear
    }

    this
  }

  def getFromDB(key: Hash): Option[Hash] = {
    db.get(key) match {
      case null => None
      case e => Some(Hash(e))
    }
  }

  // Members declared in scala.collection.MapLike
  def get(key: Hash): Option[Hash] = cache.get(key).orElse(getFromDB(key))

  def iterator: Iterator[(Hash, Hash)] = asScalaIterator(db.iterate(env.createReadTransaction())) map (
    p => (Hash(p.getKey), Hash(p.getValue)))

  def commit = {
    //tx.commit
    //tx = env.createWriteTransaction
  }
  //override def get(key: A): Option[B] = ???
  //override def iterator: Iterator[(A, B)] = ???
  //override def + [B1 >: B](kv: (A, B1)): LmdbMap[A,B] = ???
  //override def -(key: A): LmdbMap[A,B] = ???

}

// Opening and closing the database.

// try (Env env = new Env("/tmp/mydb")) {
//   try (Database db = env.openDatabase()) {
//     ... // use the db
//   }
// }
// Putting, getting, and deleting key/values.

// db.put(bytes("Tampa"), bytes("rocks"));
// String value = string(db.get(bytes("Tampa")));
// db.delete(bytes("Tampa"));
// Iterating and seeking key/values forward and backward.

// try (EntryIterator it = db.iterate()) {
//   for (Entry next : it.iterable()) {
//   }
// }

// try (EntryIterator it = db.iterateBackward()) {
//   for (Entry next : it.iterable()) {
//   }
// }

// byte[] key = bytes("London");
// try (EntryIterator it = db.seek(key)) {
//   for (Entry next : it.iterable()) {
//   }
// }

// try (EntryIterator it = db.seekBackward(key))) {
// ransaction tx = -----------------env.createWriteTransaction()) {

