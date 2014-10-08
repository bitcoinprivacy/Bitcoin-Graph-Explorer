package core

import org.bitcoinj.core.Block
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BlockFileLoader

import scala.collection.convert.WrapAsScala._

// In java that should be implements libs.BlockSource
trait BitcoinDRawFileBlockSource extends BlockSource {
        val params = MainNetParams.get
        private val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList)

        override def blockSource: Iterator[Block] = asScalaIterator(loader)
}
