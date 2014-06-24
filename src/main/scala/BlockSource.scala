import com.google.bitcoin.core.Block

trait BlockSource {
	def stream: Stream[Block]
}