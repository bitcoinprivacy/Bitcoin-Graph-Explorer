
import TestExplorer._
import org.scalatest._

class ExplorerIntegrationTests extends FlatSpec with Matchers {

  "docker" should "start bitcoin and postgres" in {
    startDocker should be (0)
  }

  it should "add 103 blocks and 1 txs to bitcoin" in {
    gen(1) should be (0)
    gen(1) should be (0)
    gen(100) should be (0)
    addTx(95) should be (0)
    gen(1) should be (0)
  }

  it should "read 0 blocks from postgres" in {
    bgeCount should be (0)
  }

  it should "read 104 blocks from bitcoin" in {
    bitcoinCount should be (104)
  }

  it should "fail sending 5000 bitcoins" in {
    addTx(5000) should be (6)
    bitcoinCount should be (104)
  }


  "explorer" should s"populate $bitcoinCount blocks" in {
    savePopulate should be (None)
    bitcoinCount should be (bgeCount)
  }

  it should "resume 1 more block with 0 txs" in {
    gen(1) should be (0)
    saveResume should be (None)
    bitcoinCount should be (bgeCount)
  }

  it should "resume 1 block with 1 tx " in {
    addTx(77) should be (0)
    gen(1) should be (0)
    saveResume should be (None)
    bitcoinCount should be (bgeCount)
  }

  for (i <- 1 to RUNS) {

    val TITLE = (if (RUNS == 1) "" else s" ($i/$RUNS)")

    it should s"resume 1 block with 4 txs$TITLE" in {
      addTx(84) should be (0)
      addTx(71) should be (0)
      addTx(71) should be (0)
      addTx(71) should be (0)
      gen(1) should be (0)
      saveResume should be (None)
      bitcoinCount should be (bgeCount)
    }

    it should s"stat are the same after rollback 5 blocks and resuming it again$TITLE" in {
      val initialStat = stat
      saveRollback(5)
      saveResume should be (None)
      stat should be (initialStat)
      bitcoinCount should be (bgeCount)
    }

    it should s"resume 2 blocks with 4 and 5 txs$TITLE" in {
      addTx(51) should be (0)
      addTx(68) should be (0)
      addTx(83) should be (0)
      addTx(51) should be (0)
      gen(1) should be (0)
      addTx(61) should be (0)
      addTx(37) should be (0)
      addTx(59) should be (0)
      addTx(83) should be (0)
      addTx(27) should be (0)
      gen(1) should be (0)
      saveResume should be (None)
      bitcoinCount should be (bgeCount)
    }
  }

  it should s"have generated $bgeCount blocks in the database" in {
    bitcoinCount should be (bgeCount)
  }
}
