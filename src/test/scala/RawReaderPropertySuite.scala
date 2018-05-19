import sys.process._
import util._
import org.scalatest._

class PopulateSpec extends FlatSpec with Matchers {
  val BITCOIN = "docker exec --user bitcoin regtestbge_bitcoin_1 bitcoin-cli -regtest -rpcuser=foo -rpcpassword=bar -rpcport=18333"
  def gen(i:Int) = (BITCOIN + " generate " + i.toString) ! ProcessLogger(_ => ())
  def resetRegtest = "/root/Bitcoin-Graph-Explorer/test.sh" ! ProcessLogger(_ => ())

  "Populate" should "safe 5 blocks as the blockchain contains 5 blocks" in {
    resetRegtest
    gen(5)
    Explorer.populate
    Explorer.blockCount should be (6)
  }

  "Resume" should "add another 10 blocks after being added to blockchain" in {
    gen(10)
    Explorer.resume
    Explorer.blockCount should be (16)
    
  }
}

