// this has all the database stuff.
package db

import slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.meta.MTable
import util._
import Hash._

trait BitcoinDB {
  def blockDB = TableQuery[Blocks]
  def addresses = TableQuery[Addresses]
  def movements = TableQuery[Movements]
  def richestAddresses = TableQuery[RichestAddresses]
  def richestClosures = TableQuery[RichestClosures]
  def stats = TableQuery[Stats]
  def utxo = TableQuery[UTXO]
  def balances = TableQuery[Balances]
  def closureBalances = TableQuery[ClosureBalances]

  def USERNAME = conf.getString("username")
  def PASSWORD = conf.getString("password")
  def HOST = conf.getString("host")
  def OPTIONS = conf.getString("jdbcOptions")
  def DBNAME = conf.getString("databaseName")
  def URL = "jdbc:postgresql://" + HOST + "/" + DBNAME + OPTIONS
  def DRIVER = "org.postgresql.Driver"

  def statsDone: Boolean = {
    DB.withSession{
      implicit session =>
      !MTable.getTables("stats").list.isEmpty && stats.length.run > 1
    }
  }

  def deleteIfExists(tables: TableQuery[_ <: Table[_]]*)(implicit session: Session) =
    tables foreach { table => if (!MTable.getTables(table.baseTableRow.tableName).list.isEmpty) table.ddl.drop }

  lazy val DB = Database.forURL(URL, user = USERNAME, password = PASSWORD, driver = DRIVER)

  def countInputs: Int =
    DB withSession { implicit session =>
      movements.length.run
    }

  def blockCount: Int = DB withSession { implicit session =>
    try {
      blockDB.length.run
    }
    catch { case e: Exception =>
      0
    }
  }

  def existsOutput(transactionHash: Hash, index: Int): Boolean =
    DB withSession { implicit session =>
      Q.queryNA[Int]("""
        select count(*) from movements where
        transaction_hash = """ + transactionHash + """ and
        `index` = """ + index).list.head > 0
    }

  def getLastBlock = DB withSession { implicit session => blockDB.sortBy(_.block_height.desc).map(a => (a.hash, a.block_height)).take(1).run.head }

  def txListQuery(blocks: Seq[Int]) = {
    val emptyArray = hashToArray(zero(0))
    DB withSession { implicit session =>
      val q = for (q <- movements.filter(_.height_out inSet blocks).filter(_.address =!= emptyArray))
        yield (q.spent_in_transaction_hash, q.address)
      // in order to read quickly from db, we need to read in the order of insertion

      q.run.toVector
    }
  }
  //  val txList = Compiled(txListQuery _) doesn't work with inSet

  def initializeReaderTables: Unit =
    {
      DB withSession { implicit session =>
        deleteIfExists(movements, blockDB, utxo)
        movements.ddl.create
        blockDB.ddl.create
        utxo.ddl.create
      }
    }

  def initializeClosureTables: Unit = {
    DB withSession { implicit session =>
      deleteIfExists(addresses)
      addresses.ddl.create
    }
  }

  def initializeStatsTables: Unit = {
    DB withSession { implicit session =>
      deleteIfExists(stats, richestAddresses, richestClosures, balances, closureBalances)

      stats.ddl.create
      richestAddresses.ddl.create
      richestClosures.ddl.create
      balances.ddl.create
      closureBalances.ddl.create

    }
  }

  
  def createBalanceTables = {
    var clock = System.currentTimeMillis
    DB withTransaction { implicit session =>

      deleteIfExists(balances, closureBalances)
      balances.ddl.create
      closureBalances.ddl.create
      // '\x' is not an address
      Q.updateNA("""insert into balances select address, sum(value) as balance from utxo where address != '\x' group by address;""").execute

      (Q.u + "create index balance on balances(balance)").execute

      Q.updateNA("""
         insert into closure_balances
              select address, sum(balance) from
                ((select a.representant as address, b.balance as balance from balances b, addresses a where b.address = a.hash and b.address != '\x')
                 UNION ALL
                (select a.address as address, a.balance as balance from balances a left outer join  addresses b on a.address = b.hash where a.address != '\x' and b.representant is null)) table2         group by address;
      """).execute // TODO: this second  part can be left out when the addresses table contains all addresses

       (Q.u + "create index balance_2 on closure_balances(balance)").execute

       Q.updateNA("""delete from balances where balance = 0;""").execute // premature optimization
       Q.updateNA("""delete from closure_balances where balance = 0;""").execute

      val b3 = balances.map(_.balance).sum.run.getOrElse(0)
      val b4 = closureBalances.map(_.balance).sum.run.getOrElse(0)

      assert(b3 == b4, s"""Wrong balances after fresh create!""")

      log.info("Balances created in %s s" format (System.currentTimeMillis - clock) / 1000)
    }
  }

  def countUTXOs = {
    DB withSession { implicit session =>
      val values = (for (u <- utxo)
        yield u.value).run.toVector

      (values.size, values.sum)
    }
  }

  def syncAddressesWithUTXOs =  DB withSession { implicit session =>
    val ads = addresses.map(_.hash)
    addresses.insert(utxo.groupBy(_.address).map(_._1).filterNot(_ in ads).map(p => (p,p)))
  }

  def getSumBalance: Long = DB withSession { implicit session =>
    balances.map(_.balance).sum.run.getOrElse(0)
  }

  def getSumWalletsBalance: Long = DB withSession { implicit session =>
    closureBalances.map(_.balance).sum.run.getOrElse(0)
  }

  def getUTXOSBalance(): Long = DB withSession { implicit session =>
    utxo.map(_.value).sum.run.getOrElse(0L)
  }

  def saveBalances(adsAndBalances: Map[Hash, Long], repsAndBalances: Map[Hash, Long], oldReps: Iterable[Hash]): Unit = {
    DB withTransaction { implicit session =>
      // delete merged wallets
      val toDelete = oldReps.map(hashToArray)
      closureBalances.filter(_.address inSet toDelete).delete
      for {
        (balances, table) <- Set((adsAndBalances, balances), (repsAndBalances, closureBalances))
        (address, balance) <- balances
      } {
        if (balance > 0L)         //  premature optimization
          table.insertOrUpdate(hashToArray(address), balance)
        else if (balance == 0)
          table.filter(_.address === hashToArray(address)).delete
      }
    }
  }

  def getRepresentant(a: Hash): Hash = DB withSession { implicit session =>
    addresses.filter(_.hash === hashToArray(a)).map(_.representant).run.map(Hash(_)).headOption.getOrElse(a)
  }

  def updateAddressDB(changedReps: Map[Hash, Set[Hash]]) = DB withTransaction { implicit session =>
    for {(rep,oldReps) <- changedReps
         if oldReps-rep != Set() // (premature?) optimization
    }
    {
      val updateQuery = for(p <- addresses if p.representant inSet (oldReps-rep).map(hashToArray _)) yield p.representant
      updateQuery.update(hashToArray(rep)) // TODO: compile query
    }
  }
    

  def saveAddresses(news: Seq[(Hash, Hash)]) = {
    
    try{
      DB withSession {
        val converted = news.map(p => (hashToArray(p._1),hashToArray(p._2)))
        addresses.insertAll(converted:_*)(_)}
    }
    catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }
    
  }
   
  def updateBalanceTables(changedAddresses: Map[Hash, Long], touchedReps: Map[Hash, Set[Hash]], changedReps: Map[Hash, Set[Hash]]):
    (Map[Hash, Long], Map[Hash, Long],Map[Hash, Long]) = {
    val clock = System.currentTimeMillis

    val adsAndBalances: Map[Hash, Long] =
      for ((address, change) <- changedAddresses - zero(0))
      yield (address, getBalance(address) + change)
 
    lazy val repsAndChanges = for ((rep,ads) <- touchedReps)
                              yield (rep,ads.toSeq.map(changedAddresses(_)).sum) // the toSeq is necessary, because otherwise equal values get swallowed in the set
       
    lazy val oldReps = changedReps.values.flatten
    lazy val newReps = repsAndChanges.keys
    // get old closure balances from DB with a single query
    lazy val oldClosureBalances = getClosureBalances(oldReps++newReps)

    def oldSum(s: Set[Hash]): Long = s.toSeq.map(a => oldClosureBalances.getOrElse(a, getBalance(a))).sum

    def total(rep: Hash) = oldSum(changedReps.getOrElse(rep, Set()))

    val repsAndBalances = for ((rep,change) <- repsAndChanges)
                          yield (rep, total(rep)+change)

    saveBalances(adsAndBalances, repsAndBalances, oldReps)

    log.info("Balances updated in %s s, %s addresses, %s µs per address "
      format
        ((System.currentTimeMillis - clock) / 1000, adsAndBalances.size, (System.currentTimeMillis - clock) * 1000 / (adsAndBalances.size + 1)))

    (adsAndBalances,repsAndChanges, repsAndBalances)

  }

  def insertRichestClosures = {

    DB withSession { implicit session =>

      val bh = blockCount - 1
      val topClosures = closureBalances.sortBy(_.balance.desc).take(richlistSize).run.toVector
      val topClosuresWithBh = for ((rep, bal) <- topClosures) yield (bh, rep, bal)

      richestClosures.insertAll(topClosuresWithBh: _*)

    }
  }

  def insertRichestAddresses = {

    
    DB withSession { implicit session =>
      Q.updateNA("""
       insert
        into richest_addresses
       select
        (select max(block_height) from blocks) as block_height,
       address,
        balance
      from
        balances
      where address!= ''
      order by
        balance desc
      limit """ + richlistSize + """
    ;""").execute

    }
  }


  def insertStatistics = {

    val startTime = System.currentTimeMillis
    val (nonDustAddresses, addressGini) = getGini(balances)
    val (nonDustClosures, closureGini) = getGini(closureBalances)

    DB withSession { implicit session =>

      List("""delete from stats where block_height = (select coalesce(max(block_height),0) from blocks);""",
           """
       insert into stats select
       (select coalesce(max(block_height),0) from blocks),
       (select coalesce(sum(balance)/100000000,0) from balances),
       (select coalesce(sum(txs),0) from blocks),
       (select count(1) from addresses),
       (select count(distinct(representant)) from addresses),
       (select count(1) from balances),
       (select count(1) from closure_balances),
       """ + nonDustAddresses + """,
       """ + nonDustClosures + """,
       """ + closureGini + """,
       """ + addressGini + """,
       """ + (System.currentTimeMillis / 1000).toString + """;""").foreach( q => (Q.u + q).execute )

      log.info("Stat created in " + (System.currentTimeMillis - startTime) / 1000 + "s");
    }
  }

  def updateStatistics(addedAds: Int, addedClosures: Int) = {

    val time = System.currentTimeMillis
    val (nonDustAddresses, addressGini) = getGini(balances)
    val (nonDustClosures, closureGini) = getGini(closureBalances)
    
    DB withSession { implicit session =>
      val stat = currentStat
      stat.total_addresses_with_balance = balances.length.run
      stat.total_closures_with_balance = closureBalances.length.run
      stat.total_addresses_no_dust = nonDustAddresses.intValue
      stat.total_closures_no_dust = nonDustClosures.intValue
      stat.gini_closure = closureGini
      stat.gini_address = addressGini
      stat.block_height = blockCount - 1
      stat.tstamp = System.currentTimeMillis / 1000
      stat.total_transactions = blockDB.map(_.txs).filter(_ > 0).sum.run.getOrElse(0).toLong
      stat.total_bitcoins_in_addresses = balances.map(_.balance).sum.run.getOrElse(0L) / 100000000
      stat.total_addresses += addedAds
      stat.total_closures += addedClosures
      saveStat(stat)

      log.info("Stat updated in " + (System.currentTimeMillis - time) / 1000 + " seconds")
    }
  }

  def getGini[A <: Table[_] with BalanceField](balanceTable: TableQuery[A]): (Long, Double) = {

    val balanceVector = DB withSession { implicit session =>
      balanceTable.map(_.balance).filter(_ > dustLimit).sorted.run.toVector
    }

    val balances = balanceVector.map(_.toDouble)

    val n: Long = balances.length

    val summe = balances.sum
    val mainSum = balances.zipWithIndex.map(p => p._1 * (p._2 + 1.0) / n).sum
    val gini: Double = if (n == 0) 0.0 else 2.0 * mainSum / (summe) - (n + 1.0) / n

    (n, gini)
  }

  def createAddressIndexes = {

    log.info("Creating indexes ...")
    val time = System.currentTimeMillis

    DB withSession { implicit session =>
      for (
        query <- List(
          "create index representant on addresses (representant)",
          "create unique index hash on addresses (hash)"
        )
      ) {
        Q.updateNA(query).execute
        log.info("Finished " + query)
      }

    }

    log.info("Indexes created in %s s" format (System.currentTimeMillis - time) / 1000)

  }

  def createIndexes = {

    log.info("Creating indexes ...")
    val time = System.currentTimeMillis

    DB withSession { implicit session =>

      for (
        query <- List(
          "create index address on movements (address);",
          """create unique index tx_idx  on movements (transaction_hash, "index");""",
          "create index spent_in on movements(spent_in_transaction_hash)", // for api/Movements
          "create index height_in on movements (height_in);", // for closure
          "create index height_out_in on movements (height_out, height_in);",
          "create index address_utxo on utxo (address)",
          "create index height_utxo on utxo (block_height)",
          "create index tx_utxo on utxo (transaction_hash, index)",
          "create index block_height on blocks(block_height);"
        )
      ) {
        Q.updateNA(query).execute
        log.info("Finished" + query)
      }
   }

    log.info("Indexes created in %s s" format (System.currentTimeMillis - time) / 1000)

  }

  def currentStat = DB withSession { implicit session =>
    Stat.tupled(stats.sortBy(_.block_height desc).firstOption.get)
  }

  def saveStat(stat: Stat) = DB withSession { implicit session =>
    stats.filter(_.block_height === stat.block_height).delete
    stats.insert(Stat.unapply(stat).get)
  }

  case class Stat(var block_height: Int,
                  var total_bitcoins_in_addresses: Long,
                  var total_transactions: Long,
                  var total_addresses: Long,
                  var total_closures: Long,
                  var total_addresses_with_balance: Long,
                  var total_closures_with_balance: Long,
                  var total_addresses_no_dust: Long,
                  var total_closures_no_dust: Long,
                  var gini_closure: Double,
                  var gini_address: Double,
                  var tstamp: Long)  {

    def toMap() = getCCParams(this)

    override def equals(that: Any): Boolean = {
      that match {
        case that: Stat => 
          this.toMap()-"tstamp" == that.toMap()-"tstamp"
        case _ =>
          false
      }
    }

    override def toString(): String = {
      getCCParams(this).toString()
    }

  }

  def lastCompletedHeight: Int = DB withSession { implicit session =>
    stats.map(_.block_height).max.run.getOrElse(0)
  }

  def getUtxosMaxHeight = DB withSession { implicit session => utxo.map(_.block_height).max.run.getOrElse(0) }

  // FIXME: rollback do not reconstruct stats, it just delete them and create it again.
  // It could be added but it does not hurt to create it again (60 minutes at the moment)
  def rollBack(blockHeight: Int) = DB withSession { implicit session =>

    log.info("rolling back block " + blockHeight)

    stats.filter(_.block_height === blockHeight).delete
    richestAddresses.filter(_.block_height === blockHeight).delete
    richestClosures.filter(_.block_height === blockHeight).delete

    val table = LmdbMap.open("utxos")
    val utxoTable = new UTXOs(table)
    val utxoQuery = utxo.filter(_.block_height === blockHeight)

    for ((tx, idx) <- utxoQuery.map(p => (p.transaction_hash, p.index)).run)
      utxoTable -= Hash(tx) -> idx
    utxoQuery.delete
    
    val movementsToDelete = movements.filter(x => x.height_out === blockHeight || x.height_in === blockHeight)
    val utxoRows = movements
      .filter(x => x.height_in === blockHeight || x.height_out===blockHeight)
      .map(p => (p.transaction_hash, p.address, p.index, p.value, p.height_in))
      .run

    for ((tx, ad, idx, v, h) <- utxoRows)
      utxoTable += ((Hash(tx) -> idx) -> (Hash(ad), v, h))

    utxo.insertAll(utxoRows: _*)
    movementsToDelete.delete
    blockDB.filter(_.block_height === blockHeight).delete
    table.close
  }

  def getAddressReps(a: Iterable[Hash]): Map[Hash, Hash] = DB withSession {implicit session =>
    addresses.filter(_.hash inSet (a.map(hashToArray _)) ).map(p => (p.hash,p.representant)).run.map(p=>(Hash(p._1),Hash(p._2))).toMap
  }

  def getClosureBalances(a: Iterable[Hash]): Map[Hash, Long] = DB withSession { implicit session =>
    closureBalances.filter(_.address inSet a.map(hashToArray(_))).map(a => (a.address, a.balance)).run.map(p=> (Hash(p._1), p._2)).toMap
  }

  def getWalletBalances(a: Set[Hash]): Long = DB withSession { implicit session =>
    closureBalances.filter(_.address inSet a.map(hashToArray(_))).map(_.balance).sum.run.getOrElse(0L)
  }

  def getBalance(a: Hash): Long = DB withSession { implicit session =>
    balances.filter(_.address === hashToArray(a)).map(_.balance).firstOption.getOrElse(0L)
  }

  def getAllBalances(): Map[Hash, Long] = DB withSession { implicit session =>
    balances.sortBy(_.address).map(p=>(p.address, p.balance)).run.map(p=>(Hash(p._1),p._2)).toMap
  }

  def getClosures(): Map[Hash, Hash] = DB withSession { implicit session =>
    addresses.sortBy(_.hash).map(p=>(p.hash, p.representant)).run.map(p=>(Hash(p._1),Hash(p._2))).toMap
  }

  def getEmptyUTXOs(): Iterable[(Hash)] = DB withSession { implicit session =>
    utxo.filter(_.value === 0L).map(_.address).run.map(Hash(_))
  }

  def getAllUTXOs(): Iterable[(Hash, Int)] = DB withSession { implicit session =>
    utxo.map(p =>(p.address, p.block_height)).run.map(p => (Hash(p._1), p._2))
  }

  def countClosures(): Int = DB withSession { implicit session =>
    addresses.groupBy(_.representant).map(p => p._1).length.run
  }

  def countAddresses(): Int = DB withSession { implicit session =>
    addresses.length.run
  }

  def getWallet(a: Hash): Set[Hash] = DB withSession { implicit session =>
    addresses.filter(_.representant === hashToArray(a)).map(_.hash).run.map(Hash(_)).toSet
  }

  def getAllWalletBalances(): Map[Hash, Long] = DB withSession { implicit session =>
    closureBalances.sortBy(_.address).map(p=>(p.address, p.balance)).run.map(p=>(Hash(p._1),p._2)).toMap
  }

  def deleteLastStats = DB withSession { implicit session =>
    stats.filter(_.block_height === blockCount - 1).delete
  }
}
