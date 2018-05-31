import sys.process._
import org.scalatest._
import util.Hash
import tools.Tool

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
      blockCount should be (0)
    }
  }

  it should "populate blocks" in {
    savePopulate
    blockCount should be (count+1)
  }

  it should "have same stats using create or update" in {
    val initialStat = currentStat
    gen(1) should be (0)
    saveResume
    currentStat.total_closures >= initialStat.total_closures should be (true)
    blockCount should be (count+1)
    gen(1) should be (0)
  }

  it should "add correclty blocks" in {
    saveResume
    blockCount should be (count+1)
  }

  it should "add blocks and txs" in {
    addTx(6) should be (0)
    gen(1) should be (0)
    gen(1) should be (0)
    addTx(7) should be (0)
    gen(1) should be (0)
    saveResume
    blockCount should be (count+1)
  }

  for (i <- 1 to 10) {
    it should "work after several rollbacks"+i in {
      val initialStat = currentStat
      rollBack(blockCount-1)
      saveResume
      blockCount should be (count+1)
      currentStat should be (initialStat)
      addTx(8)
      addTx(8)
      gen(2) should be (0)
      saveResume
      blockCount should be (count+1)
    }
  }
}
