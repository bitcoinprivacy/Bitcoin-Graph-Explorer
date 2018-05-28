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

  "Populate" should "work normally" in {
    startDocker 
    gen(1)
    gen(100)
    Explorer.populate
    Explorer.blockCount should be (102)
   }

  "Resume" should "work normally" in {
    gen(102)
    Explorer.resume
    Explorer.blockCount should be (204)

  }

  "Resume" should "work after several txs added" in {
    addTxs
    gen(10)
    gen(100)
    Explorer.resume
    addTxs
    gen(1)
    Explorer.resume
    //stopDocker // Deactivate it only if you want to analize the database after the tests ran
    Explorer.blockCount should be (315)
  }
}

