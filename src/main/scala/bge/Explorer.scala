import actions._
import util._
import sys.process._


/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/1
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
object Explorer extends App with db.BitcoinDB {
  args.toList match{
    case "start"::rest =>

      if (!statsDone || rest.headOption == Some("--force"))
        populate

      Seq("touch",lockFile).!

      iterateResume(false)

    case "populate"::rest             =>

      populate

    case "resume"::rest =>

      Seq("touch",lockFile).!

      iterateResume(rest.headOption==Some("--newstats"))

    case "stop"::rest =>

      Seq("rm",lockFile).!

    case "info"::rest =>

      getInfo

    case _=>

      println("""

        Available commands:

         start [--force]: populate if necessary or --force, then resume
         stop: ask bge gently to stop at the end of the current iteration. Note that thís can take a few days if you are in the populate phase.
         populate: create the database movements with movements and closures.  Deletes any existing data.
         resume [--newstats]: update the database generated by populate with new incoming data. Works only if populate is already done. The newstats flag forces a regeneration of balance, richlist and stats tables.
         info
      """)
  }

  def getInfo = {
    val (count, amount) = sumUTXOs
    println("Sum of the utxos saved in the lmdb: "+ amount)
    println("Total utxos in the lmdb: " + count)
    val (countDB, amountDB) = countUTXOs
    println("Sum of the utxos in the sql db " +amountDB)
    println("Total utxos in the sql db " + countDB)
  }

  def totalExpectedSatoshi(blockCount: Int): Long = {
    val epoch = blockCount/210000
    val blocksSinceEpoch = blockCount % 210000
    def blockReward(epoch: Int) = Math.floor(50L*100000000L/Math.pow(2,epoch)).toLong
    val fullEpochs = for (i <- (0 until epoch))
                     yield 210000L * blockReward(i)
    val correct = fullEpochs.sum + blocksSinceEpoch * blockReward(epoch)
    correct - blockReward(0) * (if (blockCount > 91880) 2 else if (blockCount > 191842) 1 else 0)
    // correct for the two duplicate coinbase tx (see BIP 30) that we just store once (they are unspendable anyway)
  }

  def sumUTXOs = {
    lazy val table = LmdbMap.open("utxos")
    lazy val outputMap: UTXOs = new UTXOs (table)
    // (txhash,index) -> (address,value,blockIn)
    val values = for ( (_,(_,value,_)) <- outputMap.view) yield value //makes it a lazy collection
    val tuple = values.grouped(checkUTXOsSize).foldLeft((0,0L)){
      case ((count,sum),group) =>
        //log.info(count + " elements read at ")
        val seq = group.toSeq
        (count+seq.size,sum+seq.sum)
    }

    table.close
    tuple
  }

  def populate = {

    val dataDirectory = new java.io.File(dataDir)

    dataDirectory.delete
    dataDirectory.mkdir

    initializeReaderTables
    initializeClosureTables
    initializeStatsTables

    insertStatistics

    if (!peerGroup.isRunning) startBitcoinJ

    PopulateBlockReader

    createIndexes
    new PopulateClosure(PopulateBlockReader.processedBlocks)
    createAddressIndexes
    populateStats
  }

  def resume = {

    val read = new ResumeBlockReader
    val closure = new ResumeClosure(read.processedBlocks, read.changedAddresses.toMap)

    // FIXME
    // That check is neccessary due to a bug in updateBalances
    // Delete checkBalances condition when it is fixed
    val checkBalances =  getSumBalance == getSumWalletsBalance

    if (!checkBalances)
      log.info("Error during update of balances or stats .... creating balances and stats")

    if (read.changedAddresses.size < balanceUpdateLimit && checkBalances) {
      val (adsAndBalances,repsAndChanges,  repsAndBalances) =
        resumeStats(read.changedAddresses.toMap, closure.touchedReps, closure.changedReps, closure.addedAds, closure.addedClosures)
      Some(
        (adsAndBalances,repsAndChanges,  read.changedAddresses.toMap, repsAndBalances, closure.touchedReps, closure.changedReps, closure.addedAds, closure.addedClosures)
      )
    }
    else {
      populateStats
      None
    }
  }

  def rollBackToLastStatIfNecessary: Boolean =
    getWrongBlock match {
      case None =>
        false
      case Some(block: Int) => 
        rollBack(block)
        rollBackToLastStatIfNecessary
        true
    }

  def iterateResume(newStats: Boolean) = {

    if (!peerGroup.isRunning)
      startBitcoinJ

    log.info("Checking for wrong blocks")

    val rollbacked = rollBackToLastStatIfNecessary

    if (rollbacked || newStats )
      populateStats

    while (new java.io.File(lockFile).exists) {
      if (blockCount > chain.getBestChainHeight) {
        log.info("Waiting for new blocks")
        // wait until the chain overtakes our DB
        waitForBitcoinJBlock(blockCount)
      }

      resume

    }

    log.info("Lock file deleted. BGE shut down correctly!")
  }

  def getWrongBlock: Option[Int] = {

    val lch = lastCompletedHeight
    val bc = blockCount
    if (bc-1 > lastCompletedHeight){
      log.error("Incomplete process, missing stats and maybe more data. Rollback required.")
      return Some(lch)
    }
      

    val (count,amount) = sumUTXOs
    val (countDB, amountDB) = countUTXOs
    val expected = totalExpectedSatoshi(bc)
    val utxosMaxHeight = getUtxosMaxHeight
    val sameCount = count == countDB
    val sameValue = amount == amountDB
    val rightValue = amount <= expected
    val rightBlock = (bc - 1 == lch) && utxosMaxHeight == lch

    if (!rightValue)  log.error("we have " + ((amount-expected)/100000000.0) + " too many bitcoins")
    if (!sameCount)   log.error("we lost utxos")
    if (!sameValue)   log.error("different sum of btcs in db and lmdb")
    if (!rightBlock)  log.error("wrong or incomplete block")

    if (sameCount && sameValue && rightValue && rightBlock) None
    else if (utxosMaxHeight > bc -1) Some(utxosMaxHeight)
    else if (bc - 1 > lch) Some(bc-1)
    else Some(lch)

  }

  def resumeStats(changedAddresses: Map[Hash,Long], touchedReps: Map[Hash,Set[Hash]], changedReps: Map[Hash,Set[Hash]], addedAds: Int, addedClosures: Int):
      (Map[Hash, Long],Map[Hash, Long],Map[Hash, Long]) = {

    val (adsAndBalances,repsAndChanges,  repsAndBalances) = updateBalanceTables(changedAddresses, touchedReps, changedReps)

    createRichestLists

    updateStatistics(addedAds, addedClosures)

    (adsAndBalances,repsAndChanges,  repsAndBalances)
  }

  def createRichestLists = {
    var startTime = System.currentTimeMillis
    insertRichestAddresses
    insertRichestClosures
    log.info("Richest list created in " + (System.currentTimeMillis - startTime) / 1000 + "s")
  }

  def populateStats = {
    createBalanceTables
    createRichestLists
    insertStatistics
  }

  def waitForBitcoinJBlock(b: Int) = {
    chain.getHeightFuture(b).get
  }
}
