import libs._

trait SQLBlocksDB extends BlocksDB with SQLSchemata {
  
  def savedBlockHashes = TableQuery[Blocks]
  var savedBlocksSet:Set[String] = Set.empty
    val savedBlocks =
      for (b <- Blocks)
        yield (b.hash)
    for (c <- savedBlocks)
      savedBlocksSet = savedBlocksSet + c
}