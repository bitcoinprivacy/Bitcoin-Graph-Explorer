
import TestExplorer._
import org.scalatest._

class ExplorerIntegrationTests extends FlatSpec with Matchers {

  "docker" should "start correctly" in {
    startDocker should be (0)
  }

  it should "create 103 blocks and 6 txs with bitcoin" in {
    gen(100) should be (0)
    gen(1) should be (0)
    gen(1) should be (0)
    addTx(6) should be (0)
    gen(1) should be (0)
  }

  it should "read from database" in {
    bitcoinCount should be (104)
  }

  it should "read from bitcoin" in {
    bitcoinCount should be (104)
  }

  it should "fail sending 5000 bitcoins" in {
    addTx(5000) should be (6)
    bitcoinCount should be (104)
  }

  "explorer" should "throw postgres exception before run" in {
    a [org.postgresql.util.PSQLException] should be thrownBy {
      bgeCount should be (0)
    }
  }

  it should "populate blocks" in {
    savePopulate should be (success)
    bitcoinCount should be (bgeCount)
  }

  it should "resume blocks" in {
    gen(1) should be (0)
    saveResume should be (success)
    bitcoinCount should be (bgeCount)
  }

  it should "resume more blocks" in {
    gen(1) should be (0)
    gen(1) should be (0)
    saveResume should be (success)
    bitcoinCount should be (bgeCount)
  }

  val MAX = 10

  for (i <- 1 to MAX) {

    val TITLE = s"($i/$MAX)"

    it should s"resume blocks and txs $TITLE" in {
      addTx(16) should be (0)
      addTx(8) should be (0)
      gen(2) should be (0)
      saveResume should be (success)
      bitcoinCount should be (bgeCount)
    }

    it should s"rollback blocks $TITLE" in {
      val initialStat = stat
      saveRollback(5)
      saveResume should be (success)
      stat should be (initialStat)
      bitcoinCount should be (bgeCount)
    }

    it should s"resume more blocks and txs $TITLE" in {
      addTx(6) should be (0)
      gen(1) should be (0)
      gen(1) should be (0)
      addTx(7) should be (0)
      gen(1) should be (0)
      addTx(7) should be (0)
      gen(1) should be (0)
      saveResume should be (success)
      bitcoinCount should be (bgeCount)
    }
  }

  it should s"have generated $bgeCount blocks in the database" in {
    bitcoinCount should be (bgeCount)
  }
}
