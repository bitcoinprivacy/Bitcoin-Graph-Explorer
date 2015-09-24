package core

import org.bitcoinj.core.{Block, NetworkParameters}

/**
 * Created by yzark on 25.08.14.
 */
trait BlockSource {
    def params: NetworkParameters
    def blockSource: Iterator[Block]
}
