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
    case Nil => println("What do you want?")
    case "populate"::rest => new RawBlockFileReaderUncompressed(rest)
    case "singleaddressclosure"::rest => new AddressClosure(rest)
    case "closure"::rest => new AllAddressesClosure(rest)
    case _=> println("""Available commands:
             populate [number of blocks] [init]
             singleaddressclosure [address]
             closure""")

  }

}
