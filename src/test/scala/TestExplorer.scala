import util.Hash
import Explorer._
import sys.process._
import scala.util.Random

object TestExplorer {

  // CONSTANTS

  val BITCOIN = "docker exec --user bitcoin docker_bitcoin_1 bitcoin-cli -regtest -rpcuser=foo -rpcpassword=bar -rpcport=18333 "

  val SCRIPTS = "/root/Bitcoin-Graph-Explorer/src/test/docker/"

  val RUNS = 5

  // SOME HELPERS

  private def s: collection.immutable.Map[Hash, Long] = collection.immutable.Map()

  private def x:collection.immutable.Map[Hash, Set[Hash]] = collection.immutable.Map()

  private def nr(b: Long): String = ((b/10000)/10000.0).toString

  private def sx = "\n      "

  private def sp = " "

  private def pri(a: Hash) = a.toString//.drop(2).take(4)

  private def str1(n: collection.immutable.Map[Hash, Long]): String = "(" + n.size + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1)+": "+(nr(m._2))).mkString(sx)

  private def str2(n: collection.immutable.Map[Hash, Hash]): String = "(" + n.size + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1) +" => "+(pri(m._2))).mkString(sx)

  private def str3(n: collection.immutable.Map[Hash, Set[Hash]]): String = "(" + n.size + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1) + " => "+ m._2.map((pri(_))).mkString(" - ")).mkString(sx)

  private def str4(n: collection.immutable.Map[Hash, String]): String = "(" + n.size + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1)+": "+m._2).mkString(sx)

  // BITCOIN-CLI functions

  def gen(i:Int): Int = (BITCOIN + "generate" + " " + i.toString) ! ProcessLogger(_ => ())

  def addBlocks(n: Int): Int = (1 to n).map(_ => gen(1)).sum

  def startDocker = (SCRIPTS + "start.sh") ! ProcessLogger(_ => ())

  def stopDocker = (SCRIPTS) + "stop.sh" ! ProcessLogger(_ => ())

  def pay(n: Long) = (SCRIPTS + "create.sh" + " " + n.toString) ! ProcessLogger(_ => ())

  def addTxs(n: Int) = (1 to n).map(_ => pay(Random.nextInt(49)+50)).sum

  def bitcoinCount: Int = ((BITCOIN + "getblockcount") !!).trim.toInt+1

  // EXPLORER functions

  def bgeCount = blockCount

  def totalAddresses: Int = countAddresses

  def totalClosures: Int = countClosures

  def stat = currentStat

  def safeRollback(i: Int) = {

    for (_ <- 0 until i)
      rollBack(blockCount-1)

    populateStats

    assertBalancesCreatedCorrectly
  }

  def safeResume = {

    resume match {
      case Some((a,b,c,d,e,f,x)) =>
        assertBalancesUpdatedCorrectly(a,b,c,d,e,f,x)
      case None =>
        assertBalancesCreatedCorrectly
    }

  }

  def safePopulate = {
    populate
    assertBalancesCreatedCorrectly
  }

  var errors = 0

  def assertBalancesCreatedCorrectly =
    assertBalancesUpdatedCorrectly(s,s,s,s,x,0,0)

  def assertBalancesUpdatedCorrectly(
    adsAndBalances: collection.immutable.Map[Hash, Long],
    repsAndChanges: collection.immutable.Map[Hash, Long],
    changedAddresses: collection.immutable.Map[Hash, Long],
    repsAndBalances: collection.immutable.Map[Hash, Long],
    changedReps: collection.immutable.Map[Hash, Set[Hash]],
    addedAdds: Int,
    addedReps: Int): Option[String] = {

    // skip further tests after an error

    if (errors > 0) return Some("___SKIP___")

    // values to check

    lazy val ca = changedAddresses - Hash.zero(0)
    lazy val s1 = ca.map(_._2).sum
    lazy val s2 = repsAndChanges.map(_._2).sum
    lazy val b3 = getSumBalance
    lazy val b4 = getSumWalletsBalance
    lazy val ut = getUTXOSBalance()
    lazy val x1 = getAllWalletBalances.toSeq.sortBy(_._1).filter(_._2>0).toMap
    lazy val x2 = getAllBalances.groupBy(p => getRepresentant(p._1)).map(p => (p._1, p._2.map(_._2).sum)).toSeq.sortBy(_._1).filter(_._2>0).toMap

    // strings to print

    lazy val wrongClosures = for {k <- x1.keys; if None == x2.get(k) || x2(k) != x1(k)} yield (k, nr(x1(k)))

    lazy val toDelete = (changedReps.values.fold(Set())((a, b) => a ++ b) ++ changedReps.keys ++ repsAndBalances.keys ++ adsAndBalances.keys - Hash.zero(0)).map(x=>(x,"OUT")).toMap

    lazy val wrongWalletTableString = s"""${sx}DIFF${sx}${nr(b4-b3)}${sx}WRONG ${str4(wrongClosures.toMap)}${sx}CHANGED ${str1(repsAndChanges-Hash.zero(0))}${sx}ADDED ${str1(repsAndBalances-Hash.zero(0))}${sx}DELETED ${str4(toDelete)}"""

    // test updateStatistics && test updateBalances

    lazy val errorOption =
      if (x1.exists(r => r._1 != getRepresentant(r._1)))
        Some(s"___WRONG_CLOSURE_IN_TABLE___$wrongWalletTableString")
      else if (s1 != s2)
        Some(s"___WRONG_CHANGED_VALUES___$wrongWalletTableString")
      else if (ut != b3)
        Some(s"___WRONG_ADDRESS_TABLE ___$wrongWalletTableString")
      else if (b3 != b4 || !wrongClosures.isEmpty)
        Some(s"___WRONG_WALLET_TABLE___$wrongWalletTableString")
      else if (countAddresses != currentStat.total_addresses)
        Some(s"___WRONG_TOTAL_ADDRESSES___$wrongWalletTableString")
      else if (countClosures != currentStat.total_closures)
        Some(s"___WRONG_TOTAL_WALLETS___$wrongWalletTableString")
      else
        None

    errors += (if (errorOption != None) 1 else 0)

    errorOption
  }
}
