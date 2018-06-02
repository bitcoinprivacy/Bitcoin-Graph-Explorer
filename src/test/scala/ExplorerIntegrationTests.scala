
import TestExplorer._
import org.scalatest._

class ExplorerIntegrationTests extends FlatSpec with Matchers {

  "docker" should "start bitcoin and postgres" in {
    startDocker should be (0)
  }

  it should "add 103 blocks and 1 txs to bitcoin" in {
    addBlocks(102) should be (0)
    addTxs(1) should be (0)
    addBlocks(1) should be (0)
  }

  it should "read 0 blocks from postgres" in {
    bgeCount should be (0)
  }

  it should "read 104 blocks from bitcoin" in {
    bitcoinCount should be (104)
  }

  it should "fail sending 5000 bitcoins" in {
    pay(5000) should be (6)
  }

  "explorer" should s"populate $bitcoinCount blocks" in {
    savePopulate should be (None)
  }

  it should "resume an empty block" in {
    addBlocks(1) should be (0)
    saveResume should be (None)
  }

  it should "resume a block with 1 tx" in {
    addTxs(1) should be (0)
    addBlocks(1) should be (0)
    saveResume should be (None)
  }

  it should "resume 5 blocks with several txs each" in {
    (1 to 5).foreach(i => {
      addTxs(5+i) should be (0)
      addBlocks(1) should be (0)
    })
    saveResume should be (None)
  }

  it should "rollback 8 blocks and resume it again" in {
    saveRollback(8) should be (0)
    saveResume should be (None)
  }

  it should s"have saved $bgeCount blocks" in {
    bitcoinCount should be (bgeCount)
  }

  it should s"have saved $totalClosures closures" in {
    bitcoinCount should be (bgeCount)
  }

  it should s"have saved $totalAddresses addresses" in {
    bitcoinCount should be (bgeCount)
  }
}
