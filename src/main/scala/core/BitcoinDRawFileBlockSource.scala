package core

import org.bitcoinj.core._
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BlockFileLoader

import scala.collection.convert.WrapAsScala._

// In java that should be implements libs.BlockSource
trait BitcoinDRawFileBlockSource extends BlockSource
{
  def params = MainNetParams.get
  
  private lazy val loader = {
    val context = new Context(params) // had to put this here because of scala trait initialization madness
    new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList)}
 
  override def blockSource: Iterator[Block] = asScalaIterator(loader)
}
