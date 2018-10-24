import util.Hash
import Explorer._
import sys.process._
import scala.util.Random
import org.scalatest._

object TestExplorer extends db.BitcoinDB {

  // CONSTANTS

  val BITCOIN = "docker exec --user bitcoin docker_bitcoin_1 bitcoin-cli -regtest -rpcuser=foo -rpcpassword=bar -rpcport=18333 "

  val SCRIPTS = "/root/Bitcoin-Graph-Explorer/src/test/docker/"
 
  val RUNS = 5

  // SOME HELPERS

  private def s: Map[Hash, Long] = Map()

  private def x:Map[Hash, Set[Hash]] = Map()

  private def nr(b: Long): String = ((b/10000)/10000.0).toString

  private def sx = "\n      "

  private def sp = " "

  private def pri(a: Hash) = a.toString.drop(2).take(4)

  private def str1(n: Map[Hash, Long]): String = "(" + nr(n.map(_._2).sum) + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1)+": "+(nr(m._2))).mkString(sx)

  private def str2(n: Map[Hash, Hash]): String = "(" + n.size + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1) +" => "+(pri(m._2))).mkString(sx)

  private def str3(n: Map[Hash, Set[Hash]]): String = "(" + n.size + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1) + " => "+ m._2.map((pri(_))).mkString(" - ")).mkString(sx)

  private def str4(n: Map[Hash, String]): String = "(" + n.size + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1)+": "+m._2).mkString(sx)

  // BITCOIN-CLI functions

  def gen(i:Int): Int = (BITCOIN + "generate" + " " + i.toString) ! ProcessLogger(_ => ())

  def addBlocks(n: Int): Int = (1 to n).map(_ => gen(1)).sum

  def startDocker = (SCRIPTS + "start.sh") ! ProcessLogger(_ => ())

  def stopDocker = (SCRIPTS) + "stop.sh" ! ProcessLogger(_ => ())

  def pay(n: Long) = (SCRIPTS + "create.sh" + " " + n.toString) ! ProcessLogger(_ => ())

  def addTxs(n: Int) = (1 to n).map(_ => pay(Random.nextInt(49)+50)).sum

  def bitcoinCount: Int = ((BITCOIN + "getblockcount") !!).trim.toInt+1

  def bitcoinTotal: Double = ((BITCOIN + "getbalance") !!).trim.toDouble * 100000000

  def bgeTotal: Long = getSumBalance

  // EXPLORER functions

  def bgeCount = blockCount

  def totalAddresses: Int = countAddresses

  def totalClosures: Int = countClosures

  def stat = currentStat

  def safeRollback() = {

    deleteLastStats
    // rollback do not reconstruct stats, it just delete them and create it again
    rollBackToLastStatIfNecessary
    populateStats
    assertBalancesCreatedCorrectly
  }

  def safeResume = {

    resume match {
      case Some((a,b,c,d,e,y,f,x)) =>
        assertBalancesUpdatedCorrectly(a,b,c,d,e,y,f,x)
      case None =>
        assertBalancesCreatedCorrectly
    }
  }

  def safePopulate = {
    populate
    assertBalancesCreatedCorrectly
  }

  def assertBalancesCreatedCorrectly =
    assertBalancesUpdatedCorrectly(s,s,s,s,x,x,0,0)

  def assertBalancesUpdatedCorrectly(
    adsAndBalances: Map[Hash, Long],
    repsAndChanges: Map[Hash, Long],
    changedAddresses: Map[Hash, Long],
    repsAndBalances: Map[Hash, Long],
    touchedReps: Map[Hash, Set[Hash]],
    changedReps: Map[Hash, Set[Hash]],
    addedAdds: Int,
    addedReps: Int): Option[String] = {

    // values to check
    lazy val ca = changedAddresses - Hash.zero(0)
    lazy val s1 = ca.map(_._2).sum
    lazy val s2 = repsAndChanges.map(_._2).sum
    lazy val b3 = getSumBalance
    lazy val b4 = getSumWalletsBalance
    lazy val ut = getUTXOSBalance()
    lazy val x1 = getAllWalletBalances//.filter(_._2>0)
    lazy val x2 = getAllBalances.groupBy(p => getRepresentant(p._1)).map(p => (p._1, p._2.map(_._2).sum))//.filter(_._2>0)
    lazy val allClosures = getClosures.groupBy(_._2).map(p => (p._1, p._2.keySet))
    lazy val emptyUtxos = getEmptyUTXOs()

    // strings to print
    lazy val wrongClosures = for {k <- x1.keys; if None == x2.get(k) || x2(k) != x1(k)} yield (k, x1(k))
    lazy val wrongWalletTableString = /*s"""
        ${sx}TOUCHED REPS${str3(touchedReps)}
        ${sx}WRONG reps${str1(wrongClosures.toMap)}
        ${sx}CHANGED ADDs${str1(ca)}
        ${sx}CHANGED REPRESENTANTS ${str3(changedReps)}
        ${sx}REPS AND BALANCES${str1(repsAndBalances)}
        ${sx}COUNT WALLETS CREATE, UPDATE ${countClosures()} ${currentStat.total_closures}
        ${sx}WALLETS ${str3(allClosures)}
        ${sx}EMPTY UTXOS ${sx}(${emptyUtxos.size})${sx}${emptyUtxos.map(pri(_))}
                                       */
      s"""
        ${sx}BALANCES ADD - REP - UTXOs${sx}${nr(b3)} ${nr(b4)} ${nr(ut)}
        ${sx}Stats ${sx}${currentStat}
        ${sx}UTXOs ${sx}${getAllUTXOs.filter(_._2 == currentStat.block_height).map(p=>pri(p._1)+": "+p._2)}
"""

    lazy val countsDiffBitcoinToBge = bitcoinTotal - b3

    // test updateStatistics && test updateBalances
    lazy val errorOption =
      if (false && emptyUtxos.size > 0)
        Some(s"___EMPTY UTXOs___$wrongWalletTableString")
      else if (b3 != b4 || !wrongClosures.isEmpty)
        Some(s"___WRONG_WALLET_TABLE___$wrongWalletTableString")
      else if (x1.exists(r => r._1 != getRepresentant(r._1)))
        Some(s"___WRONG_CLOSURE_IN_TABLE___$wrongWalletTableString")
      else if (s1 != s2)
        Some(s"___WRONG_CHANGED_VALUES___$wrongWalletTableString")
      else if (ut != b3)
        Some(s"___WRONG_ADDRESS_TABLE ___$wrongWalletTableString")
      else if (countAddresses != currentStat.total_addresses)
        Some(s"___WRONG_TOTAL_ADDRESSES___$wrongWalletTableString")
      else if (countClosures != currentStat.total_closures)
        Some(s"___WRONG_TOTAL_WALLETS___$wrongWalletTableString")
      else
        None

    errorOption
  }
}

object CancelGloballyAfterFailure {
  @volatile var cancelRemaining = false
}

trait CancelGloballyAfterFailure extends TestSuiteMixin { this: TestSuite =>
  import CancelGloballyAfterFailure._

  abstract override def withFixture(test: NoArgTest): Outcome = {
    if (cancelRemaining)
      Canceled("Canceled by CancelGloballyAfterFailure because a test failed previously")
    else
      super.withFixture(test) match {
        case failed: Failed =>
          cancelRemaining = true
          failed
        case outcome => outcome
      }
  }

  final def newInstance: Suite with OneInstancePerTest = throw new UnsupportedOperationException
}
