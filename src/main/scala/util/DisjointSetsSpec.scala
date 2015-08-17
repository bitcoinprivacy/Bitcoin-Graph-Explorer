package util


class DisjointSetsSpec[A] {

  var set: Set[Set[A]] = Set.empty



  def union(i: A, j: A): DisjointSetsSpec[A] = {
    val s1 = set.find(_.contains(i))
    val s2 = set.find(_.contains(j))
    set = (s1, s2) match {
      case (Some(s1), Some(s2)) =>
        (((set - s1) - s2) + (s1 ++ s2))
      case _ =>
        set
    }
    this
  }

  def add(i: A): DisjointSetsSpec[A] = {
    set =
      if (set.exists(_.contains(i)))  set
      else set + Set(i)
    this
  }

  def find(i: A): (Option[A], DisjointSetsSpec[A]) = {

    val repOpt = (for (s <-  set.find(_.contains(i)) ) yield s.head)
    (repOpt, this)
    // we assume that head is deterministic.
    // Note that we do not need an implementation to return the same
    // representing element as our specification.
    // We only need to make sure that in any implementation,
    // the same representing element is returned for i in the same set.
    // So we cannot directly test the find implementation against this example implementation,
    // but we can test that the DisjointSets returned do not change.
  }
}
