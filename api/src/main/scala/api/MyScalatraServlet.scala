package api

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

class MyScalatraServlet extends BgeapiStack with db.BitcoinDB with JacksonJsonSupport with CorsSupport {

  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  
  }
    //  options("/*"){
    // response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
 // /}

  // get("/") {
  //   <html>
  //     <body>
  //       <h1>Hello, BGE lightning ball!</h1>
  //       Say <a href="hello-scalate">hello to Scalate</a>.
  //     </body>
  //   </html>
  // }


  def ad = Hash(hexAddress(params("ad")))
  def tx = Hash(hex2bytes(params("tx")))
  def block_height = params("block_height").toInt
  def from = params("from").toInt
  def until = params("until").toInt
  def limit = params("limit").toLong
  // BLOCKS

  get("/blocks/:from/:until") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
    Block.getBlocks(from, until)
  }

  get("/blocks/summary"){
    Block.getSummary
  }

  get("/utxos/:ad/:from/:until") {
    UTXO.getUtxosByAd(ad,from,until)
  }

  get("/utxos/:ad/summary") {
    UTXO.getUtxosByAdSummary(ad)
  }

  get("/tx_utxos/:tx/:from/:until") {
    UTXO.getUtxosByTx(tx,from,until)
  }

  get("/tx_utxos/:tx/summary") {
    UTXO.getUtxosByTxSummary(tx)
  }

  get("/movements/:ad/:from/:until") {
    Movement.getMovements(ad,from,until)
  }

  get("/movements/:ad/summary") {
    Movement.getMovementsSummary(ad)
  }


  get("/inputs/:tx/:from/:until") {
    Movement.getInputs(tx,from,until)
  }

  get("/inputs/:tx/summary") {
    Movement.getInputsSummary(tx)
  }

  get("/outputs/:tx/:from/:until") {
    Movement.getOutputs(tx,from,until)
  }

  get("/outputs/:tx/summary") {
    Movement.getOutputsSummary(tx)
  }

  get("/stats") {
    Stats.getStats
  }

  get("/stats/history") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
    Stats.getAllStats
  }

  get("/wallet/:ad/:from/:until"){
    Address.getWallet(ad,from,until)
  }

  get("/wallet/:ad/summary"){
    Address.getWalletSummary(ad)
  }

  get("/txs/:block_height/:from/:until"){
    Transaction.get(block_height, from, until)
  }

  get("/txs/:block_height/summary"){
    Transaction.getInfo(block_height)
  }

  get("/richlist/addresses/:block_height/:from/:until") {
    Address.getAddressList(richestAddresses,block_height,from,until)
  }

  get("/richlist/wallets/:block_height/:from/:until") {
    Address.getAddressList(richestClosures,block_height,from,until)
  }

  get("/richlist/addresses/:block_height/summary") {
    Address.getAddressListSummary(richestAddresses,block_height)
  }

  get("/richlist/wallets/:block_height/summary") {
    Address.getAddressListSummary(richestClosures,block_height)
  }

  
  get("/distribution/:limit") {
    Distribution.get(limit)
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
