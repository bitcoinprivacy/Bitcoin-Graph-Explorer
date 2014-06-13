import actions._

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/13
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
object Explorer extends App{
  args.toList match{
    case "populate"::rest             => new RawBlockFileReaderUncompressed(rest)
    case "closure"::rest              => new AllAddressesClosure(rest)
    case "balance"::rest              => new AllAddressesBalance(rest)
    case "all"::rest                  =>
      val populater = new RawBlockFileReaderUncompressed(if (rest.isEmpty) List("100000", "init") else rest)
      new CreateIndex(List())
      new AllAddressesClosure(List(populater.start.toString, populater.end.toString))
    case "createindexes"::rest        => new CreateIndex(rest)
    case _=> println
    ("""
      Available commands:
      populate [number of blocks] [init]
      balance
      closure
      createindexes
      all [number of blocks]
    """)
  }
}
