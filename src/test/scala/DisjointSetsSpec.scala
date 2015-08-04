import util._
import org.scalacheck._
import Arbitrary.arbitrary



object DisjointSetsSpec extends org.scalacheck.Commands {

  // This is our system under test. All commands run against this instance.
  var unionFind = new DisjointSets[Int](scala.collection.mutable.Map.empty)



  // This is our state type that encodes the abstract state. The abstract state
  // should model all the features we need from the real state, the system
  // under test. We should leave out all details that aren't needed for
  // specifying our pre- and postconditions. The state type must be called
  // State and be immutable.
  case class State(ds: Set[Set[Int]]){
    def equals(uf: DisjointSets[Int]): Boolean = {
      val setOfMaps = uf.elements.groupBy(_._2._2).values.toSet
      val setOfSets = setOfMaps map (_.keys.toSet)
      setOfSets == ds
    }
  }

  // initialState should reset the system under test to a well defined
  // initial state, and return the abstract version of that state.
  def initialState() = {
    unionFind.elements.clear
    val empty: Set[Set[Int]] = Set.empty
    State(empty)
  }

  // We define our commands as subtypes of the traits Command or SetCommand.
  // Each command must have a run method and a method that returns the new
  // abstract state, as it should look after the command has been run.
  // A command can also define a precondition that states how the current
  // abstract state must look if the command should be allowed to run.
  // Finally, we can also define a postcondition which verifies that the
  // system under test is in a correct state after the command execution.

  case class Add(i: Int) extends Command {
    def run(s: State) = {unionFind = unionFind.add(i); unionFind}
    def nextState(s: State) =
      if (s.ds.exists(_.contains(i))) s
      else new State(s.ds + Set(i))

    // if we want to define a precondition, we add a function that
    // takes the current abstract state as parameter and returns a boolean
    // that says if the precondition is fulfilled or not. In this case, we
    // have no precondition so we just let the function return true. Obviously,
    // we could have skipped adding the precondition at all.
    preConditions += (s => true)
    postConditions += {
      case(s0,s1: State, ut: DisjointSets[Int]) => s1.equals(ut)
    }
  }

  case class Find(i: Int) extends Command {
    def run(s: State) = {val (e,uF) = unionFind.find(i) ; unionFind = uF; (e, uF)}
    def nextState(s: State) = s

    preConditions += (s => s.ds.exists(_.contains(i)))
    postConditions += {
      case(s0,s1: State, pair: (Int, DisjointSets[Int])) => s1.ds.exists(s => s.contains(i) && s.contains(pair._1))
    }
    postConditions += {
      case(s0,s1: State, pair: (Int, DisjointSets[Int])) => s1.equals(pair._2)
    }
  }

  case class Union(i: Int, j: Int) extends Command {
    def run(s: State) = {unionFind = unionFind.union(i,j); unionFind}
    def nextState(s: State) = {
      val s1 = s.ds.find(_.contains(i))
      val s2 = s.ds.find(_.contains(j))
      State(s.ds - s1.get - s2.get + (s1.get ++ s2.get))
    }

    preConditions += (s => s.ds.exists(_.contains(i)))
    preConditions += (s => s.ds.exists(_.contains(j)))
    // when we define a postcondition, we add a function that
    // takes three parameters, s0, s1 and r. s0 is the abstract state before
    // the command was run, s1 is the state after the command was run
    // and r is the result from the command's run method. The
    // postcondition function should return a Boolean (or
    // a Prop instance) that says if the condition holds or not.

    postConditions += {
      case(s0,s1: State, pair: (Int, DisjointSets[Int])) => s1.equals(pair._2)
    }
  }

  // This is our command generator. Given an abstract state, the generator
  // should return a command that is allowed to run in that state. Note that
  // it is still neccessary to define preconditions on the commands if there
  // are any. The generator is just giving a hint of which commands that are
  // suitable for a given state, the preconditions will still be checked before
  // a command runs. Sometimes you maybe want to adjust the distribution of
  // your command generator according to the state, or do other calculations
  // based on the state.
  def genCommand(s: State): Gen[Command] =
    for { i <- arbitrary[Int]
      j <- arbitrary[Int]
      c <- Gen.oneOf(Add(i), Find(i), Union(i,j))
    } yield c


}
