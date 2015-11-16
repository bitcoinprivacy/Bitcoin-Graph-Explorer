package util

import scala.collection.convert.WrapAsScala._
import org.fusesource.lmdbjni._
import org.fusesource.lmdbjni.Constants._
import scala.collection.mutable.Map
import Hash._

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

  var env = new Env()
  env.open(conf.getString("mdb.path") + "/" + name, NOSYNC)
  env.setMapSize(1024 * 1024 * 1024 * 1024L) // 1TB
  var db = env.openDatabase("test")

  // TODO: review why it is not working to use tx directly instead of caching the elements
  val cache: Map[Hash,Hash] = Map.empty
  val transactionSize = conf.getInt("lmbdTransactionSize")

  override def empty: LmdbMap = new LmdbMap

  def -=(key: Hash): LmdbMap.this.type = {
    cache -= key
    db.delete(key)

    this
  }

  def +=(kv: (Hash, Hash)): LmdbMap.this.type = {

    cache += kv

    if (cache.size == transactionSize)
      commit

    this
  }

  // Members declared in scala.collection.MapLike
  def get(key: Hash): Option[Hash] = cache.get(key).orElse(getFromDB(key))

  var tx: Option[Transaction] = None

  implicit class TxAbortingIterator(i: Iterator[(Hash,Hash)]) extends Iterator[(Hash, Hash)]
  {
    override def hasNext = {
      val hN = i.hasNext

      if (!hN)
        for (t <- tx) {
          t.abort
          tx = None
        }

      hN
    }

    override def next = i.next
  }

  def iterator: TxAbortingIterator =  {
    commit

    for (t <- tx)
      t.abort

    tx = Some(env.createReadTransaction)
   
    asScalaIterator(db.iterate(tx.get)) map (
        p => (Hash(p.getKey), Hash(p.getValue)))
  }

  override def size: Int ={
    commit
    db.stat.getEntries.toInt
  }

  def getFromDB(key: Hash): Option[Hash] = {
    val result = tx match { // check if we have a read tx open
      case Some(tx) =>
        db.get(tx, key)
      case None =>
        db.get(key)
    }

    result match {
      case null => None
      case e => Some(Hash(e))
    }
  }


  def commit = {
    val t = System.currentTimeMillis
    val tx = env.createWriteTransaction
    for (kv <- cache)
      db.put(tx, kv._1, kv._2)
    tx.commit
    println("commit took " + (System.currentTimeMillis - t) + " ms")
    cache.clear
  }

  def close = {
    commit
    env.close
  }
}
