import sys.process._
import util._
import org.scalatest._

class BGESpec extends FlatSpec with Matchers {
  val BITCOIN = "docker exec --user bitcoin docker_bitcoin_1 bitcoin-cli -regtest -rpcuser=foo -rpcpassword=bar -rpcport=18333"
  // fixme: to use an script is dirty but we need to access to bitcoincli which lives in a container
  def gen(i:Int) = (BITCOIN + " generate " + i.toString) ! ProcessLogger(_ => ())
  def resetRegtest = "/root/Bitcoin-Graph-Explorer/src/test/docker/reset.sh" ! ProcessLogger(_ => ()) 
  // fixme: to use an script is dirty but i need to restart some containers...

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

