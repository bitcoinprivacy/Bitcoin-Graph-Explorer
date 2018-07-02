import TestExplorer._
import org.scalatest._

////////////////////////////////////////////////////////////////////////////////////////////////
//
// The functions "safeRollback", "safePopulate" and "safeResume" are just a wrapper for the original
// Explorer functions, but adding an exhaustive analysis of all the generated data (in case of
// updateStatistis - updateBalances).
//
// Definitions can be found in TestExplorer.
//
// After each operation, several data will be checked:
//
// UPDATE
//
// consistency of repsAndBalances
// consistency of repsAndChanges
// consistency of addedReps
// consistency of addedAdds
// consistency of changedAddresses
// consistency of adsAndBalances
// consistency of changedReps
// total balances of wallets against addresses and utxos
// total_closures and total_addresses are the same as with createStatistics
//
// CREATE // ROLLBACK
//
// total balances of wallets against addresses and utxos
//
// NOTE: atm there is a bug leading the balances to be different than the "closureBalances".
// In order to fix the code we need first a test to cover this issue. I could not yet
// reproduce the error.
//
//////////////////////////////////////////////////////////////////////////////////////////////

class ExplorerIntegrationTests extends FlatSpec with Matchers with CancelGloballyAfterFailure{

  "docker" should "start bitcoin and postgres" in {
    startDocker should be (0)
  }

  it should "create 103 blocks and 5 txs" in {
    addBlocks(102) should be (0)
    pay(8) should be (0)
    pay(7) should be (0)
    pay(6) should be (0)
    pay(5) should be (0)
    pay(4) should be (0)
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
    safePopulate should be (None)
  }

  it should "resume a block with 5 txs" in {
    pay(19)
    pay(17)
    pay(8)
    pay(14)
    pay(100)
    addBlocks(1) should be (0)
    safeResume should be (None)
  }

  it should "resume a block with 1 tx" in {
    addTxs(1) should be (0)
    addBlocks(1) should be (0)
    safeResume should be (None)
  }

  it should "resume a block with 7 txs" in {
    addTxs(7) should be (0)
    addBlocks(1) should be (0)
    safeResume should be (None)
  }

  it should "rollback once and resume it again" in {
    val init = stat
    safeRollback() should be (None)
    safeResume should be (None)
    init should be (stat)
  }
 
  it should "resume 2 mores block with 6 and 0 txs" in {
    addTxs(6) should be (0)
    addBlocks(2) should be (0)
    safeResume should be (None)
  }

  it should "rollback again and resume it again" in {
    val init = stat
    safeRollback() should be (None)
    safeResume should be (None)
    init should be (stat)
  }

  List((50, 2), (1, 90
       )).foreach{ case (blocks: Int, txs: Int) => {

    it should s"resume $blocks blocks with $txs txs each" in {

      (1 to blocks).foreach(i => {
        addTxs(txs) should be (0)
        addBlocks(1) should be (0)
      })

      safeResume should be (None)

    }

  }}

  it should "rollback 8 blocks and resume it again" in {
    val init = stat
    safeRollback() should be (None)
    safeResume should be (None)
    stat should be (init)
  }

  it should "resume a block with 8 txs" in {
    addTxs(8) should be (0)
    addBlocks(1) should be (0)
    safeResume should be (None)
  }

  it should s"finish" in {
    println(s"Created postgres DB with $bgeCount blocks, $totalAddresses addresses and $totalClosures wallets")
  }

}
