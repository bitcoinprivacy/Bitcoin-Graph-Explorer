import sys.process._
import util._
import org.scalatest._

class BGESpec extends FlatSpec with Matchers {

  // FIXME: Use bitcoin rpc client and docker tools instead 

  val BITCOIN = "docker exec --user bitcoin docker_bitcoin_1 bitcoin-cli -regtest -rpcuser=foo -rpcpassword=bar -rpcport=18333"

  def gen(i:Int) = (BITCOIN + " generate " + i.toString) ! ProcessLogger(_ => ())

  def startDocker = "/root/Bitcoin-Graph-Explorer/src/test/docker/start.sh" ! ProcessLogger(_ => ())

  def stopDocker = "/root/Bitcoin-Graph-Explorer/src/test/docker/stop.sh" ! ProcessLogger(_ => ())

  def addTxs = "/root/Bitcoin-Graph-Explorer/src/test/docker/create.sh" ! ProcessLogger(_ => ())

  "Populate" should "save 10 blocks as the blockchain contains 10 blocks" in {
    startDocker 
    gen(1)
    gen(100)
    Explorer.populate
    Explorer.blockCount should be (102)
   }

  "Resume" should "after add 10 blocks, resume save the 10 blocks" in {
    gen(1)
    gen(1)
    Explorer.resume
    Explorer.blockCount should be (104)
    gen(100)
    Explorer.resume
    addTxs
    gen(10)
    Explorer.resume
    gen(25)
    Explorer.resume
    Explorer.blockCount should be (239)
    // Test config?
    //stopDocker // Deactivate it only if you want to analize the database after the tests ran
    true
  }
}

