import libs.Hash

trait BlocksDB {
	def savedBlockHashes: Set[Hash]
}