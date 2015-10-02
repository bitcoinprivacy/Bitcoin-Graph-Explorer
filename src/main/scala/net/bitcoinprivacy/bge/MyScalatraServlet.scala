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


class MyScalatraServlet extends BgeStack  with JacksonJsonSupport  {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats


  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  
  }

  get("/") {
    <html>
      <body>
        <h1>Hello, BGE lightning ball!</h1>
        Say <a href="hello-scalate">hello to Scalate</a>.
      </body>
    </html>
  }

  get("/blocks") {
    Block.getBlocks(1)
  }

  get("/utxos/:ad") {
    UTXO.getUTXOs(Hash(hexAddress(params("ad"))),1)
  }

  get("/tx_utxos/:tx") {
    UTXO.getUTXOsByTransaction(Hash(hex2bytes(params("tx"))),1)
  }


  get("/movements/:ad") {
    Movement.getMovements(Hash(hexAddress(params("ad"))),1)
  }

  get("/movements/:ad") {
    Movement.getMovements(Hash(hexAddress(params("ad"))),1)
  }

  get("/inputs/:tx") {
    Movement.getInputs(Hash(hex2bytes(params("tx"))),1)
  }

  get("/outputs/:tx") {
    Movement.getOutputs(Hash(hex2bytes(params("tx"))),1)
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
