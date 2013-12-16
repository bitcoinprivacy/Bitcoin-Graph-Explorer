package actions

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/13
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
import libs._
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

class AddressClosure(args:List[String]){
  databaseSession  {

      val address = if (args.length > 0 && args(0) != "0") args(0) else "1XPTgDRhN8RFnzniWCddobD9iKZatrvH4"




      println(recursiveExtend(Set(address)))
      println(recursiveExtend(Set(address)).size)

      def getRelatedAddresses(address:String):Set[String] = {

        val pizzaAddTra =
          for {
            o <- RawOutputs if o.address === address
            i<- RawInputs   if i.output_transaction_hash === o.transaction_hash && i.output_index === o.index
          } yield(i.transaction_hash)

        val traInpAdd =
          for {
            transactionHash <- pizzaAddTra
            i <- RawInputs if i.transaction_hash === transactionHash
            o <- RawOutputs if i.output_transaction_hash === o.transaction_hash && i.output_index === o.index
          } yield o.address

        var addressSet:Set[String] = Set.empty
        for (a <- traInpAdd)
            addressSet+=a

        //println(address +" => " +addressSet.toString)
        addressSet-="0"
        addressSet
      }
      def extendAddressSet(addressSet:Set[String]):Set[String] = {

        (for (a <- addressSet)
          yield
            getRelatedAddresses(a)
        ).reduceOption((a,b)=>a.union(b)).getOrElse(Set.empty)
      }
      def recursiveExtend(addressSet:Set[String]):Set[String] = {
        if (addressSet == Set(1)) return Set.empty
        val nextSet = extendAddressSet(addressSet)
        //println(addressSet)
        if (nextSet==addressSet) nextSet
          else recursiveExtend(nextSet)
      }
  }


}
