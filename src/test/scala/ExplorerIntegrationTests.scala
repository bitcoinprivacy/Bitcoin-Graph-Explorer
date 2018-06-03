import TestExplorer._
import org.scalatest._

class ExplorerIntegrationTests extends FlatSpec with Matchers {

  "docker" should "start bitcoin and postgres" in {
    startDocker should be (0)
  }

  it should "create 103 blocks and 5 txs" in {
    addBlocks(102) should be (0)
    addTxs(5) should be (0)
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

  "explorer" should "populate 104 blocks" in {
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

  it should "resume a block with 7 txs" in {
    addTxs(2) should be (0)
    addBlocks(1) should be (0)
    saveResume should be (None)
  }

  it should "rollback 3 blocks and resume it again" in {
    saveRollback(8) should be (None)
    saveResume should be (None)
  }

  List((3, 6), (4, 3)).foreach{ case (blocks: Int, txs: Int) => {

    it should s"resume $blocks blocks with $txs txs each" in {

      (1 to blocks).foreach(i => {
        addTxs(txs) should be (0)
        addBlocks(1) should be (0)
      })

      saveResume should be (None)

    }

  }}

  it should "rollback 4 blocks and resume it again" in {
    saveRollback(8) should be (None)
    saveResume should be (None)
  }

  it should "resume a block with 8 txs" in {
    addTxs(2) should be (0)
    addBlocks(1) should be (0)
    saveResume should be (None)
  }

  it should s"finish" in {
    println(s"Created postgres DB with $bgeCount blocks, $totalAddresses addresses and $totalClosures wallets")
  }
}
