
import com.google.bitcoin.core.Block

trait BlockSource {
    def blockSource: Iterator[Block]
}
