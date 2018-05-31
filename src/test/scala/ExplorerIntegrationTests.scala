
import TestExplorer._
import org.scalatest._

class ExplorerIntegrationTests extends FlatSpec with Matchers {

  "docker" should "start correctly" in {
    startDocker should be (0)
  }

  it should "communicate bitcoin data" in {
    gen(100) should be (0)
    gen(1) should be (0)
    gen(1) should be (0)
    addTx(6) should be (0)
    gen(1) should be (0)
  }

  "explorer" should "throw postgres exception if there are no blocks" in {
    a [org.postgresql.util.PSQLException] should be thrownBy {
      bgeCount should be (0)
    }
  }

  it should "populate blocks" in {
    savePopulate
    bgeCount should be (bitcoinCount)
  }

  it should "have same stats using create or update" in {
    gen(1) should be (0)
    saveResume should be (None)
    bgeCount should be (bitcoinCount)
    gen(1) should be (0)
  }

  it should "add correclty blocks" in {
    saveResume should be (None)
    bgeCount should be (bitcoinCount)
  }

  it should "add blocks and txs" in {
    addTx(6) should be (0)
    gen(1) should be (0)
    gen(1) should be (0)
    addTx(7) should be (0)
    gen(1) should be (0)
    addTx(7) should be (0)
    gen(1) should be (0)
    saveResume should be (None)
    bgeCount should be (bitcoinCount)
  }

  it should "work after several rollbacks" in {
    val initialStat = stat
    rollBackLast
    saveResume should be (None)
    bgeCount should be (bitcoinCount)
    stat should be (initialStat)
    addTx(8)
    addTx(8)
    gen(2) should be (0)
    saveResume should be (None)
    bgeCount should be (bitcoinCount)
  }
}
