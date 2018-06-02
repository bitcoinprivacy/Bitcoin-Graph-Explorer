import util.Hash
import Explorer._
import sys.process._

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

  private def pri(a: Hash) = a.toString.drop(2).take(4)

  private def str1(n: collection.immutable.Map[Hash, Long]): String = "(" + n.size + ") total: " + nr(n.map(_._2).sum) + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1)+": "+(nr(m._2))).mkString(sx)

  private def str2(n: collection.immutable.Map[Hash, Hash]): String = "(" + n.size + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1) +" => "+(pri(m._2))).mkString(sx)

  private def str3(n: collection.immutable.Map[Hash, Set[Hash]]): String = "(" + n.size + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1) + " => "+ m._2.map((pri(_))).mkString(" - ")).mkString(sx)

  private def str4(n: collection.immutable.Map[Hash, String]): String = "(" + n.size + ")" + sx + n.toSeq.sortBy(p=>pri(p._1)).map(m=>pri(m._1)+": "+m._2).mkString(sx)

  // BITCOIN-CLI functions

  def gen(i:Int): Int = (BITCOIN + "generate" + " " + i.toString) ! ProcessLogger(_ => ())

  def startDocker = (SCRIPTS + "start.sh") ! ProcessLogger(_ => ())

  def stopDocker = (SCRIPTS) + "stop.sh" ! ProcessLogger(_ => ())

  def addTx(n: Long) = (SCRIPTS + "create.sh" + " " + n.toString) ! ProcessLogger(_ => ())

  def bitcoinCount: Int = ((BITCOIN + "getblockcount") !!).trim.toInt+1

  // EXPLORER functions

  def bgeCount = blockCount

  def stat = currentStat

  def saveRollback(i: Int) = {
    for (_ <- 0 until i)
      rollBack(blockCount-1)
    populateStats
  }

  def saveResume = {
    val init = currentStat
    resume match {
      case Some((a,b,c,d,e,f,x,y)) =>
        assertBalancesUpdatedCorrectly(a,b,c,d,e,f,x,y)
      case None =>
        assertBalancesCreatedCorrectly
    }
  }

  def savePopulate = {
    populate
    assertBalancesCreatedCorrectly
  }

  def assertBalancesCreatedCorrectly =
    assertBalancesUpdatedCorrectly(s,s,s,s,s,x,0,0)

  def assertBalancesUpdatedCorrectly(
    repsAndAvailable: collection.immutable.Map[Hash, Long],
    adsAndBalances: collection.immutable.Map[Hash, Long],
    repsAndChanges: collection.immutable.Map[Hash, Long],
    changedAddresses: collection.immutable.Map[Hash, Long],
    repsAndBalances: collection.immutable.Map[Hash, Long],
    changedReps: collection.immutable.Map[Hash, Set[Hash]],
    addedAdds: Int,
    addedReps: Int): Option[String] = {

    // values to check

    lazy val ca = changedAddresses - Hash.zero(0)
    lazy val s1 = ca.map(_._2).sum
    lazy val s2 = repsAndChanges.map(_._2).sum
    lazy val b3 = getSumBalance
    lazy val b4 = getSumWalletsBalance
    lazy val ut = getUTXOSBalance()
    lazy val wb = getAllWalletBalances.filter(p=>getWallet(t1._1) contains p._1)
    lazy val ar = addedReps
      - (changedReps.values.foldLeft(Set[Hash]())((s,p) => s++p)--changedReps.keys).size
      + changedReps.keys.size
    lazy val negativeAddressOption = adsAndBalances.filter(_._2 < 0).map(p => (pri(p._1), nr(p._2))).headOption
    lazy val negativeWalletOption = repsAndBalances.filter(_._2 < 0).headOption
    lazy val balanceResults = s"UTXOs ${nr(ut)} ADDs ${nr(b3)} WALLETs ${nr(b4)}"
    lazy val t1 = negativeWalletOption.getOrElse((Hash.zero(10), 0L))

    // strings to print
    lazy val negativeWalletString = s"\nWALLET BALANCE: ${str1(wb)} \nCHANGED REPS: ${str3(changedReps)} \nREPS AND BALANCES ${str1(repsAndBalances.filter(p=>getWallet(t1._1) contains p._1))}\n"
    lazy val x1 = getAllWalletBalances.toSeq.sortBy(_._1).toMap
    lazy val x2 = getAllBalances.groupBy(p => getRepresentant(p._1)).map(p => (p._1, p._2.map(_._2).sum)).toSeq.sortBy(_._1).toMap
    lazy val errors = for {k <- x2.keys; if x2(k) != 0 && x2(k) != x1(k)} yield (k, nr(x2(k)) + " " + nr(x1(k)))
    lazy val wrongWalletTableString = s"\nWRONG ${str4(errors.toMap)} DIFF: ${b4 - b3}"

    // test updateStatistics && test updateBalances
    if (repsAndBalances.exists(r => r._1 != getRepresentant(r._1)))
      Some(s"___ WRONG UPDATE ___")
    else if (s1 != s2)
      Some(s"___ WRONG CHANGED VALUES ___")
    else if (addedAdds < 0)
      Some(s"___ LOST ADDRESSES ___")
    else if (negativeAddressOption != None)
      Some(s"___ NEGATIVE ADDRESS ___")
    else if (ut != b3)
      Some(s"___ WRONG ADDRESS TABLE ___")
    else if (ar < 0)
      Some(s"___ LOST $ar REPRESENTANTS ___ ")
    else if (b3 != b4)
      Some(s"___ WRONG WALLET TABLE ___ $wrongWalletTableString")
    else if (negativeWalletOption != None)
      Some(s"___ NEGATIVE WALLET ___ $negativeWalletString")
    else if (!errors.isEmpty) {
      Some(s"--- WRONG WALLET TABLE --- $wrongWalletTableString")
    }
      None
  }
}
