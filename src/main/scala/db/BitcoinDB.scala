// this has all the database stuff.

package db

import slick.driver.PostgresDriver.simple._
import slick.jdbc.{StaticQuery => Q}
import slick.jdbc.meta.MTable

import util._
import collection.mutable.Map

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
    blockDB.length.run
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
    val emptyArray = Hash.zero(0).array.toArray
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
    DB withSession { implicit session =>
      log.info("Creating balances")
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
                (select a.address as address, a.balance as balance from balances a left outer join  addresses b on a.address = b.hash where a.address != '\x' and b.representant is null)) table2
         group by address;
      """).execute

       (Q.u + "create index balance_2 on closure_balances(balance)").execute

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

  def updateAdsBalancesTable[A <: Table[(Array[Byte], Long)] with AddressField](adsAndBalances: scala.collection.immutable.Map[Hash, Long], balances: TableQuery[A]): Unit = {
    DB withSession { implicit session =>
      for {
        (address, balance) <- adsAndBalances
        addressArray = Hash.hashToArray(address)
      }
      if (balance != 0L)
        balances.insertOrUpdate(addressArray, balance)
      else
        balances.filter(_.address === addressArray).delete
    }
  }

  def assertBalancesChangedCorrectly(repsAndChanges: scala.collection.immutable.Map[Hash, Long], changedAddresses: scala.collection.immutable.Map[Hash, Long], repsAndBalances: scala.collection.immutable.Map[Hash, Long], changedReps: scala.collection.immutable.Map[Hash, Set[Hash]]): Unit =
    DB withSession { implicit session =>
      for ((rep, balance) <- repsAndBalances) {

        val oldReps: Set[Hash] = if (balance < 0) changedReps.getOrElse(rep, Set())+rep else Set()

        assert(balance >= 0, s"""
                Start balance: ${rep.toString.take(8)} ${closureBalances.filter(_.address inSet oldReps.map(Hash.hashToArray(_))).map(_.balance).sum.run.getOrElse(0L)}
                Change balance: ${repsAndChanges.getOrElse(rep, 0)}
                Final balance: ${rep.toString.take(8)} $balance
                ADDRESSES ${changedAddresses.map(_._2).sum} changed ${changedAddresses.size-1}
                ${if (changedAddresses.size < 21) (changedAddresses - Hash.zero(0)).toList.map(p => p._1.toString.take(8) + ": "+ p._2).mkString("\n                ")}
                WALLETS ${repsAndChanges.map(_._2).sum} changed ${repsAndChanges.size}
                ${if (repsAndChanges.size < 20) repsAndChanges.toList.map(p=> p._1.toString.take(8) + ": "+ p._2).mkString("\n                ")}
                UNIONS (from/to) ${changedReps.map(_._2.size).sum}/${changedReps.size}
                ${if (changedReps.map(_._2.size).sum < 50) changedReps.map(p => "Representant " +p._1.toString.take(8) +"\n                "+ p._2.map(_.toString.take(8)).mkString("\n                ")).mkString("\n                ") else ""}
            """)
      }

      val s1 = changedAddresses.filter(_._1 != Hash.zero(0)).map(_._2).sum
      val s2 = repsAndChanges.map(_._2).sum

      assert(s1 == s2, s"""
                Address changed $s1 (${changedAddresses.size})
                  ${if (changedAddresses.size < 20) changedAddresses.toList.map(p => p._1.toString.take(8) + ": "+ p._2).mkString("\n                ")}
                Wallets changed $s2 (${repsAndChanges.size})
                  ${if (repsAndChanges.size < 20) repsAndChanges.toList.map(p=> p._1.toString.take(8) + ": "+ p._2).mkString("\n                ")}
                Representants/New reps: ${changedReps.map(_._2.size).sum} ${changedReps.size} ${if (changedReps.map(_._2.size).sum < 50) changedReps.map(p => p._1.toString.take(8) + "("+ p._2.size +")").mkString("         \n") else ""}
      """)
  }

  def checkBalances(): Boolean = DB withSession { implicit session =>
    val b3 = balances.map(_.balance).sum.run.getOrElse(0)
    val b4 = closureBalances.map(_.balance).sum.run.getOrElse(0)

    if (b3 != b4) {
      log.info("Incomplete or wrong balances from last run")
      false
    } else {
      log.info("Balances seems to be correct")
      true
    }
  }

  def saveBalances(adsAndBalances: scala.collection.immutable.Map[Hash, Long], repsAndBalances: scala.collection.immutable.Map[Hash, Long], changedReps: scala.collection.immutable.Map[Hash, Set[Hash]]): Unit =
    DB withSession { implicit session =>
      updateAdsBalancesTable(adsAndBalances, balances)
      updateAdsBalancesTable(repsAndBalances, closureBalances)
      // delete merged wallets
      val toDelete = changedReps.values.fold(Set())((a, b) => a ++ b).map(Hash.hashToArray(_))
      closureBalances.filter(_.address inSetBind toDelete).delete
    }

  def getWalletBalance(a: Set[Hash]): Long = DB withSession { implicit session =>
    closureBalances.filter(_.address inSet a.map(Hash.hashToArray(_))).map(_.balance).sum.run.getOrElse(0L)
  }

  def getBalance(a: Hash): Long = DB withSession { implicit session =>
    balances.filter(_.address === Hash.hashToArray(a)).map(_.balance).firstOption.getOrElse(0L)
  }

  def updateBalanceTables(changedAddresses: collection.immutable.Map[Hash, Long], changedReps: collection.immutable.Map[Hash, Set[Hash]]): Unit = {
    val clock = System.currentTimeMillis
    log.info("Updating balances ...")
    currentStat.total_bitcoins_in_addresses += changedAddresses.map { _._2 }.sum
    lazy val table = LmdbMap.open("closures")
    lazy val unionFindTable = new ClosureMap(table)
    lazy val closures = new DisjointSets[Hash](unionFindTable)
    if (!checkBalances()) {
      createBalanceTables
      return
    }

    val adsAndBalances: scala.collection.immutable.Map[Hash, Long] =
      for ((address, change) <- changedAddresses - Hash.zero(0))
         yield (address, getBalance(address) + change)

    val repsAndChanges: collection.immutable.Map[Hash, Long] = (changedReps ++ (changedAddresses - Hash.zero(0))).map(_._1).toList
        .distinct.map(rep => changedReps.find(_._2 contains rep).map(_._1).getOrElse(rep))
        .distinct.map(r => {(r, changedAddresses.filter(x => (changedReps.getOrElse(r, Set())+r) contains x._1).map(_._2).sum)})
        .toMap

    val repsAndBalances: scala.collection.immutable.Map[Hash, Long] =
      (for {
        (rep, change) <- repsAndChanges.toList
        repe = closures.find(rep)._1.getOrElse(rep)
        oldReps = changedReps.getOrElse(rep, Set())+rep+Hash(repe)
        balance = getWalletBalance(oldReps)
      } yield {
        (repe, balance + change)
      }).toMap

    assertBalancesChangedCorrectly(repsAndChanges, changedAddresses, repsAndBalances, changedReps)
    // update database
    saveBalances(adsAndBalances, repsAndBalances, changedReps)

    log.info("%s balances updated in %s s, %s µs per address "
      format
       (adsAndBalances.size, (System.currentTimeMillis - clock) / 1000, (System.currentTimeMillis - clock) * 1000 / (adsAndBalances.size + 1)))
  }

  def insertRichestClosures = {
    log.info("Calculating richest closure list...")
    var startTime = System.currentTimeMillis
    DB withSession { implicit session =>
      val bh = blockCount - 1
      val topClosures = closureBalances.sortBy(_.balance.desc).take(richlistSize).run.toVector
      val topClosuresWithBh = for ((rep, bal) <- topClosures) yield (bh, rep, bal)
      richestClosures.insertAll(topClosuresWithBh: _*)
      log.info("RichestList calculated in " + (System.currentTimeMillis - startTime) / 1000 + "s")
    }
  }

  def insertRichestAddresses = {
    log.info("Calculating richest address list...")
    var startTime = System.currentTimeMillis
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
      log.info("RichestList calculated in " + (System.currentTimeMillis - startTime) / 1000 + "s")
    }
  }

  def insertStatistics = {

    val (nonDustAddresses, addressGini) = getGini(balances)
    val (nonDustClosures, closureGini) = getGini(closureBalances)

    log.info("Calculating stats...")

    val startTime = System.currentTimeMillis
    DB withSession { implicit session =>
      val query = """
       insert
       into stats select
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
       """ + (System.currentTimeMillis / 1000).toString + """;"""

      (Q.u + query).execute
      log.info("Stats calculated in " + (System.currentTimeMillis - startTime) / 1000 + "s");

    }
  }

  def updateStatistics(changedReps: Map[Hash, Set[Hash]], addedAds: Int, addedReps: Int) = {

    log.info("Updating stats")
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
      stat.total_closures += addedReps - changedReps.values.map(_.size).sum
      saveStat(stat)
      log.info("Updated in " + (System.currentTimeMillis - time) / 1000 + " seconds")
    }
  }

  def getGini[A <: Table[_] with BalanceField](balanceTable: TableQuery[A]): (Long, Double) = {
    log.info("calculating Gini: " + balanceTable)
    val time = System.currentTimeMillis

    val balanceVector = DB withSession { implicit session =>
      balanceTable.map(_.balance).filter(_ > dustLimit).sorted.run.toVector
    }

    val balances = balanceVector.map(_.toDouble)

    val n: Long = balances.length

    val summe = balances.sum
    val mainSum = balances.zipWithIndex.map(p => p._1 * (p._2 + 1.0) / n).sum
    val gini: Double = if (n == 0) 0.0 else 2.0 * mainSum / (summe) - (n + 1.0) / n
    log.info("gini calculated in " + (System.currentTimeMillis - time) / 1000 + "s")
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

      // MOVEMENTS
      // get outputs from address
      for (
        query <- List(
          "create index address on movements (address);",
          """create unique index tx_idx  on movements (transaction_hash, "index");""",
          //  "create index  spent_in_transaction_hash2 on movements (spent_in_transaction_hash, address);",
          // not needed for closure
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
    stats.insert(Stat.unapply(stat).get)
  }

  case class Stat(var block_height: Int, var total_bitcoins_in_addresses: Long, var total_transactions: Long, var total_addresses: Long, var total_closures: Long, var total_addresses_with_balance: Long, var total_closures_with_balance: Long, var total_addresses_no_dust: Long, var total_closures_no_dust: Long, var gini_closure: Double, var gini_address: Double, var tstamp: Long)

  def lastCompletedHeight: Int = DB withSession { implicit session =>
    stats.map(_.block_height).max.run.getOrElse(0)
  }

  def getUtxosMaxHeight = DB withSession { implicit session => utxo.map(_.block_height).max.run.getOrElse(0) }

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

    val movementQuery = movements.filter(_.height_out === blockHeight)
    val utxoRows = movementQuery.filter(_.height_in =!= blockHeight).map(p => (p.transaction_hash, p.address, p.index, p.value, p.height_in)).run
    for ((tx, ad, idx, v, h) <- utxoRows)
      utxoTable += ((Hash(tx) -> idx) -> (Hash(ad), v, h))
    utxo.insertAll(utxoRows: _*)
    movementQuery.delete

    blockDB.filter(_.block_height === blockHeight).delete

    table.close

  }
}
