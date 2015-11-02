package net.bitcoinprivacy.bge

import util.Hash
import org.scalatra._
import scalate.ScalateSupport
  // JSON-related libraries
import org.json4s.{DefaultFormats, Formats}
// JSON handling support from Scalatra
import org.scalatra.json._
import net.bitcoinprivacy.bge.models._
import org.bitcoinj.core.{Address => BitcoinJAddress}
import org.bitcoinj.params.MainNetParams
import org.scalatra.CorsSupport

class MyScalatraServlet extends BgeStack  with core.BitcoinDB with JacksonJsonSupport with CorsSupport {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats


  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  
  }
  options("/*"){
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
  }

  get("/") {
    <html>
      <body>
        <h1>Hello, BGE lightning ball!</h1>
        Say <a href="hello-scalate">hello to Scalate</a>.
      </body>
    </html>
  }

  implicit def paramConv(pName: String) = params(pName).toInt
  def ad = Hash(hexAddress(params("ad")))
  def tx = Hash(hex2bytes(params("tx")))
  def block_height = "block_height"
  def from = "from"
  def until = "until" 
  // BLOCKS

  get("/blocks/:from/:until") {
    Block.getBlocks(from, until)
  }

  get("/blocks/summary"){
    Block.getSummary
  }

  get("/utxos/:ad/:from/:until") {
    UTXO.getUTXOs(ad,from,until)
  }

  get("/tx_utxos/:tx/:from/:until") {
    UTXO.getUTXOsByTransaction(tx,from,until)
  }


  get("/movements/:ad/:from/:until") {
    Movement.getMovements(ad,from,until)
  }

  get("/inputs/:tx/:from/:until") {
    Movement.getInputs(tx,from,until)
  }

  get("/outputs/:tx/:from/:until") {
    Movement.getOutputs(tx,from,until)
  }

  get("/stats") {
    Stats.getAdvancedStats
  }

  get("/stats/historial") {
//    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
    Stats.getStats
  }

  get("/wallet/:ad/:from/:until"){
    Address.getWallet(ad,from,until)
  }

  get("/txs/:block_height/:from/:until"){
    Transaction.get(block_height, from, until)
  }

  get("/txs/:block_height/summary"){
    Block.getBlocks(block_height, (block_height: Int) + 1).headOption
  }

  get("/richlist/addresses/:from/:until") {
    Address.getAddressList(richestAddresses,from,until)
  }

  get("/richlist/wallets/:from/:until") {
    Address.getAddressList(richestClosures,from,until)  
  }

  def hexAddress(stringAddress: String): String = {
    val arrayAddress = stringAddress.split(",")
    if (arrayAddress.length == 1) {
      val address = new BitcoinJAddress(MainNetParams.get, stringAddress)
                                       (if(address.isP2SHAddress) "05" else "00")+valueOf(address.getHash160)
    }
    else{
      "0" + arrayAddress.length + 
        (for (i <- 0 until arrayAddress.length) 
         yield  valueOf(new BitcoinJAddress(MainNetParams.get, arrayAddress(i)).getHash160) ).mkString("")
    }
  }

  def valueOf(buf: Array[Byte]): String = buf.map("%02X" format _).mkString

  def hex2bytes(hex: String): Array[Byte] = {
    hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }
}
