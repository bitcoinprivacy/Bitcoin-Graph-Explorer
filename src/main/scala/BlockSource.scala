
import com.google.bitcoin.core.Block

trait BlockSource {
        def blockStream: Stream[Block]
}
