package actions

import slick.driver.PostgresDriver.simple._
import core._
import util._
import Hash._

object PopulateBlockReader extends FastBlockReader with PeerSource {
  // txhash -> ((index -> (address,value)),blockIn)
  // need to override this here to get the specialized type
  override lazy val table: LmdbMap = LmdbMap.create("utxos")

  override def post = {
    saveUnmatchedOutputs
    super.post
    table.close
  }

  def saveUnmatchedOutputs: Unit =
    for (group <- outputMap.view.grouped(populateTransactionSize))
      DB withSession { implicit session =>
        utxo.insertAll((for (((transactionHash,index), (address, value, blockHeight)) <- group.toSeq)
                        yield (hashToArray(transactionHash),hashToArray(address), index, value,blockHeight)):_*)
      }
}
