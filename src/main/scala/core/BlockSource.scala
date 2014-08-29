package core

import com.google.bitcoin.core.{Block, NetworkParameters}

/**
 * Created by yzark on 25.08.14.
 */
trait BlockSource {
    val params: NetworkParameters
    def blockSource: Iterator[Block]
}
