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
    case "populate"::rest             => new BlocksReader(rest)
    case "closure"::rest              => new AddressesClosurer(rest)
    case "balance"::rest              => new AddressesBalancer(rest)
    case "all"::rest                  =>
      val populater = new BlocksReader(if (rest.isEmpty) List("100000", "init") else rest)
      val queries:List[String] = List(
    		  """create index if not exists address on movements (address)""",
    		  """create index if not exists transaction_hash_i on movements (transaction_hash, `index`)""",
    		  """create index if not exists spent_in_transaction_hash on movements (transaction_hash, spent_in_transaction_hash)""",
    		  """analyze;"""
    		  )
      new IndexCreator(queries)
      new AddressesClosurer(List(populater.start.toString, populater.end.toString))
      new IndexCreator(List(
    		  """create index if not exists representant on addresses (representant)""",
    		  """create unique index if not exists hash on addresses (hash)""",
    		  """analyze;"""
          ))
      // new AddressesBalancer(rest) isn't really needed and just bloats database
    case "createindexes"::rest        => new IndexCreator(rest)
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
