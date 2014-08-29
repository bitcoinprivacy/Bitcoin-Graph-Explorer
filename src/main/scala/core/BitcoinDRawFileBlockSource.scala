package core

import com.google.bitcoin.core.Block
import com.google.bitcoin.params.MainNetParams
import com.google.bitcoin.utils.BlockFileLoader

import scala.collection.convert.WrapAsScala._

// In java that should be implements libs.BlockSource
trait BitcoinDRawFileBlockSource extends BlockSource {
        val params = MainNetParams.get
        private val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList)
        
        override def blockSource: Iterator[Block] = asScalaIterator(loader)
}
