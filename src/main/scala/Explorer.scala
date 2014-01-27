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
    case "singleaddressclosure"::rest => new AddressClosure(rest)
    case "singleaddressbalance"::rest => new AddressBalance(rest)
    case "closure"::rest              => new AllAddressesClosure(rest)
    case "balance"::rest              => new AllAddressesBalance(rest)

    case "createindexes"::rest        => new CreateIndex(rest)
    case _=> println("""
             Available commands:
             populate [number of blocks] [init]
             singleaddressclosure [address]
             singleaddressbalance [address]
             balance
             closure
             createindexes
                     """)

  }

}
