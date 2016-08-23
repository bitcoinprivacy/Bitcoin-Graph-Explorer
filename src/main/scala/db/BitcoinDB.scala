// this has all the database stuff.
package db

import slick.driver.PostgresDriver.simple._
import slick.jdbc.{ StaticQuery => Q }
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

  def txListQuery(blocks: Seq[Int]) = {
    val emptyArray = Hash.zero(0).array.toArray
    DB withSession { implicit session =>
      val q  = for (q <- movements.filter(_.height_out inSet blocks).filter(_.address =!= emptyArray))
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
      println("DEBUG: Creating balances")
      deleteIfExists(balances, closureBalances)
      balances.ddl.create
      closureBalances.ddl.create

      Q.updateNA("insert into balances select address, sum(value) as balance from utxo group by address;").execute

      (Q.u + "create index addresses_balance on balances(address)").execute
        (Q.u + "create index balance on balances(balance)").execute

      Q.updateNA("insert into closure_balances select a.representant, sum(b.balance) as balance from balances b, addresses a where b.address = a.hash group by a.representant;").execute
      //Q.updateNA("insert into closure_balances select a.address, a.balance from balances a left outer join  addresses b on a.address = b.hash where b.representant is null").execute
      (Q.u + "create index addresses_balance_2 on closure_balances(address)").execute
        (Q.u + "create index balance_2 on closure_balances(balance)").execute

      println("DONE: Balances created in %s s" format (System.currentTimeMillis - clock)/1000)
    }
  }

  def countUTXOs = {
    DB withSession { implicit session =>
      val values = (for (u <- utxo)
      yield
        u.value).run.toVector

      (values.size, values.sum)
    }
  }

  def updateBalanceTables(changedAddresses: Map[Hash,Long], changedReps: Map[Hash,Set[Hash]]) = {
    var clock = System.currentTimeMillis
    println("DEBUG: Updating balances ...")
    currentStat.total_bitcoins_in_addresses+=changedAddresses.map{_._2}.sum

    DB withSession { implicit session =>
 
      val adsAndBalances = for ((address, change) <- changedAddresses)
                           yield (address,
                                  balances.filter(_.address === Hash.hashToArray(address)).
                                    map (_.balance).firstOption.getOrElse(0L) + change)

      updateAdsBalancesTable(adsAndBalances, balances)

      val table = LmdbMap.open("closures")
      val unionFindTable = new ClosureMap(table)
      val closures = new DisjointSets[Hash](unionFindTable)

      val repsAndChanges: collection.mutable.Map[Hash,Long] = collection.mutable.Map()
      for ((address, change) <- changedAddresses - Hash.zero(0))
      {
        val repOpt = closures.find(address)._1

        for (rep <- repOpt){ // only consider existing closures, because we omit trivial ones
          val newBalance = repsAndChanges.getOrElse(rep, 0L) + change 
          repsAndChanges += (rep -> newBalance)
        }
      }

      val repsAndBalances: Map[Hash,Long] =
        for {(rep, change) <- repsAndChanges
             oldReps = (changedReps.getOrElse(rep,Set())+rep).map(Hash.hashToArray(_))
        }
        yield (rep,
               closureBalances.filter(_.address inSetBind oldReps).
                 map (_.balance).sum.run.getOrElse(0L) + change)

      updateAdsBalancesTable(repsAndBalances, closureBalances)

      // delete all oldReps that have been unified into new ones
      val toDelete = changedReps.values.fold(Set())((a,b) => a ++ b).map(Hash.hashToArray(_))
      closureBalances.filter(_.address inSet toDelete).delete

      table.close

      println("DONE: %s balances updated in %s s, %s µs per address "
                format
                (adsAndBalances.size, (System.currentTimeMillis - clock)/1000, (System.currentTimeMillis - clock)*1000/(adsAndBalances.size+1)))
    
    }
  }

  private def updateAdsBalancesTable[A <: Table[(Array[Byte], Long)] with AddressField]
    (adsAndBalances: Map[Hash,Long], balances: TableQuery[A]): Unit = {
    for {
      (address, balance) <- adsAndBalances
      addressArray = Hash.hashToArray(address)
    }
    if (balance != 0L)
      DB withSession (balances.insertOrUpdate(addressArray, balance)(_))
    else
      DB withSession { implicit session =>
        balances.filter(_.address === addressArray).delete}
  }

  def insertRichestClosures = {
    println("DEBUG: Calculating richest closure list...")
    var startTime = System.currentTimeMillis
    DB withSession { implicit session =>
      val bh = blockCount-1
      val topClosures = closureBalances.sortBy(_.balance.desc).take(1000).run.toVector
      val topAddresses = richestAddresses.sortBy(_.block_height.desc).take(1000).run.toVector
      val repsAndBalances = topAddresses map { p =>  
        (addresses.filter(_.hash === p._2).map(_.representant).firstOption.getOrElse(p._2),p._3)
      }
      val topClosureReps = topClosures map (p => Hash(p._1)) //need Hash in order to compare
      val filtered = for {
        (rep,bal) <- repsAndBalances
        if (!topClosureReps.contains(Hash(rep)))
      } yield (rep, bal)

      val mixed = filtered ++ topClosures
      val mixedWithBh = for ((rep,bal) <- mixed) yield (bh,rep,bal)
      richestClosures.insertAll(mixedWithBh: _*)

      println("RichestList calculated in " + (System.currentTimeMillis - startTime)/1000 + "s")
    }
  }

  def insertRichestAddresses = {

    println("DEBUG: Calculating richest address list...")
    var startTime = System.currentTimeMillis

    DB withSession { implicit session =>
      Q.updateNA( """
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
      limit 1000
    ;""").execute
      println("RichestList calculated in " + (System.currentTimeMillis - startTime)/1000 + "s")
    }
  }

  def insertStatistics = {

    val (nonDustAddresses,addressGini) = getGini(balances)
    val (nonDustClosures, closureGini) = getGini(closureBalances)

    println("DEBUG: Calculating stats...")

      val startTime = System.currentTimeMillis
      DB withSession { implicit session =>
        val query =   """
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
       """+ (System.currentTimeMillis/1000).toString +""";"""

        (Q.u + query).execute
        println("DONE: Stats calculated in " + (System.currentTimeMillis - startTime)/1000 + "s");

    }
  }

  def updateStatistics(changedReps: Map[Hash,Set[Hash]], addedAds: Int, addedReps: Int) = {

    println("Updating stats")
    val time = System.currentTimeMillis
    val (nonDustAddresses, addressGini) = getGini(balances)
    val (nonDustClosures, closureGini) = getGini(closureBalances)

    /* block_height 
       total_bitcoins_in_addresses XXX
       total_transactions YYY
       total_addresses XXX
       total_closures XXX
       total_addresses_with_balance YYY
       total_closures_with_balance YYY
       total_addresses_no_dust YYY
       total_closures_no_dust YYY
       gini_address 
       gini_closure 
       tstamp      */

    // Right now with update statistics we update the values with XXX in a faster way than reading all the database. We can improve it moving the rest (YYY) to a better position, or even using psql triggers.
    // A first approach could be to modify the values direct in ResumeBlockReader, ResumeClosure (balances can be updated whenever a utxo is added or removed)
    // Update should only add the ginis and call saveStat
    DB withSession { implicit session =>
      val stat = currentStat
      stat.total_addresses_with_balance=balances.length.run
      stat.total_closures_with_balance=closureBalances.length.run
      stat.total_addresses_no_dust = nonDustAddresses.intValue
      stat.total_closures_no_dust = nonDustClosures.intValue
      stat.gini_closure=closureGini
      stat.gini_address=addressGini
      stat.block_height=blockCount-1
      stat.tstamp=System.currentTimeMillis/1000
      stat.total_transactions = blockDB.map(_.txs).filter(_ > 0).sum.run.getOrElse(0).toLong

      stat.total_addresses += addedAds
      stat.total_closures += addedReps - changedReps.values.map(_.size).sum
      saveStat(stat)
      println("Updated in " + (System.currentTimeMillis - time)/1000 + " seconds")
    }
  }

  def getGini[A <: Table[_] with BalanceField](balanceTable: TableQuery[A]): (Long, Double) = {
    println("DEBUG: calculating Gini: " + balanceTable + java.util.Calendar.getInstance().getTime())
    val time = System.currentTimeMillis

    val balanceVector = DB withSession { implicit session =>
       balanceTable.map(_.balance).filter(_ > dustLimit).sorted.run.toVector
    }

    val balances = balanceVector.map(_.toDouble)

    val n: Long = balances.length

    val summe = balances.sum
    val mainSum = balances.zipWithIndex.map(p => p._1*(p._2+1.0)/n).sum
    val gini:Double = if (n==0) 0.0 else 2.0*mainSum/(summe) - (n+1.0)/n
    println("DONE: gini calculated in " + (System.currentTimeMillis - time)/1000 + "s")
    (n, gini)
  }

  def createAddressIndexes = {

    println("DEBUG: Creating indexes ...")
    val time = System.currentTimeMillis

    DB withSession { implicit session =>
      for (query <- List(
             "create index representant on addresses (representant)",
             "create unique index hash on addresses (hash)"
           ))
      {
        Q.updateNA(query).execute
        println("DEBUG: Finished "+ query)
      }


    }

    println("DONE: Indexes created in %s s" format (System.currentTimeMillis - time)/1000)

  }

  def createIndexes = {

    println("DEBUG: Creating indexes ...")
    val time = System.currentTimeMillis

    DB withSession { implicit session =>

      // MOVEMENTS

      // get outputs from address
      for (query <- List(
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
              "create index block_height on blocks(block_height);"))

      {
        Q.updateNA(query).execute
        println("DEBUG: Finished"+ query)
      }

    }

    println("DONE: Indexes created in %s s" format (System.currentTimeMillis - time)/1000)

  }

  def currentStat = DB withSession { implicit session =>
    Stat.tupled(stats.sortBy(_.block_height desc).firstOption.get)
  }

  def saveStat(stat: Stat) = DB withSession { implicit session =>
    stats.insert(Stat.unapply(stat).get)
  }

  case class Stat (var block_height: Int, var total_bitcoins_in_addresses: Long, var total_transactions: Long, var total_addresses: Long, var total_closures: Long, var total_addresses_with_balance: Long, var total_closures_with_balance: Long, var total_addresses_no_dust: Long, var total_closures_no_dust: Long, var gini_closure: Double, var gini_address: Double, var tstamp: Long)
  
  def lastCompletedHeight: Int = DB withSession { implicit session =>
    stats.map(_.block_height).max.run.getOrElse(0)
  }

  def rollBack = DB withSession { implicit session =>

    val blockHeight = blockCount - 1
    stats.filter(_.block_height === blockHeight).delete
    richestAddresses.filter(_.block_height === blockHeight).delete
    richestClosures.filter(_.block_height === blockHeight).delete
    val table = LmdbMap.open("utxos")
    val utxoTable = new UTXOs(table)

    val utxoQuery = utxo.filter(_.block_height === blockHeight)

    for ((tx,idx) <- utxoQuery.map(p => (p.transaction_hash,p.index)).run)
      utxoTable -= Hash(tx) -> idx
    utxoQuery.delete

    val movementQuery = movements.filter(_.height_out === blockHeight)
    val utxoRows = movementQuery.filter(_.height_in =!= blockHeight).map(p => (p.transaction_hash,p.address, p.index, p.value, p.height_in)).run
    for ((tx,ad,idx,v,h) <- utxoRows)
      utxoTable += ((Hash(tx) -> idx) -> (Hash(ad),v,h))
    utxo.insertAll(utxoRows:_*)
    movementQuery.delete

    blockDB.filter(_.block_height === blockHeight).delete

    table.close
  }
}
