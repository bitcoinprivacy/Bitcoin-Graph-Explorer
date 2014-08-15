import com.google.bitcoin.params.MainNetParams
import com.google.bitcoin.utils.BlockFileLoader
import scala.collection.convert.WrapAsScala._
import com.google.bitcoin.core.Block

// In java that should be implements BlockSource
trait BitcoinDRawFileBlockSource extends BlockSource {
        private val params = MainNetParams.get
        private val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList)
        
        override def blockSource: Iterator[Block] = asScalaIterator(loader)
}
