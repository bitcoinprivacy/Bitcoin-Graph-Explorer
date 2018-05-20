import sys.process._
import util._
import org.scalatest._

class BGESpec extends FlatSpec with Matchers {
  val BITCOIN = "docker exec --user bitcoin docker_bitcoin_1 bitcoin-cli -regtest -rpcuser=foo -rpcpassword=bar -rpcport=18333"
  // fixme: to use an script is dirty but we need to access to bitcoincli which lives in a container
  def gen(i:Int) = (BITCOIN + " generate " + i.toString) ! //ProcessLogger(_ => ())

  def startDocker = "/root/Bitcoin-Graph-Explorer/src/test/docker/start.sh" ! //ProcessLogger(_ => ()) 

  def stopDocker = "/root/Bitcoin-Graph-Explorer/src/test/docker/stop.sh" ! //ProcessLogger(_ => ())

  // fixme: to use an script is dirty but i need to restart some containers...
  // for some reason starting bge 2 times in same process fails... maybe cause singleton pattern?

  "Populate" should "save 10 blocks as the blockchain contains 10 blocks" in {
    startDocker
    gen(10)
    Explorer.populate
    Explorer.blockCount should be (11)
  }

  "Resume" should "after add 10 blocks, resume save the 10 blocks" in {
    gen(10)
    Explorer.resume
    val result = Explorer.blockCount
    stopDocker
    result should be (21)
  }
}

