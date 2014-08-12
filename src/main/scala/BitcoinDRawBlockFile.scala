import com.google.bitcoin.params.MainNetParams
import com.google.bitcoin.utils.BlockFileLoader
import scala.collection.convert.WrapAsScala._
import com.google.bitcoin.core.Block

trait BitcoinDRawBlockFile extends BlockSource {
        private val params = MainNetParams.get
        private val loader = new BlockFileLoader(params,BlockFileLoader.getReferenceClientBlockFileList)
        
        override def blockStream: Iterator[Block] = asScalaIterator(loader)   
}
