
import com.google.bitcoin.core._

trait BlockSource {
    val params: NetworkParameters
    def blockSource: Iterator[Block]
}
