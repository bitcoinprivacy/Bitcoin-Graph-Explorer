package util

import scala.collection.mutable.Map

// A functional, tail recursive implementation of a union-find structure

class DisjointSets[A](val elements: Map[A,(Int,A)] = Map[A,(Int,A)]()) extends DisjointSetsSpec[A] {

  override def add(element:A): DisjointSets[A] = {
    if (!elements.contains(element))
      elements+=(element->(0,element))
    this
  }

  def asSet: Set[Set[A]] = {
    val setOfMaps = elements.groupBy(_._2._2).values.toSet
    val setOfSets = setOfMaps map (_.keys.toSet)
    setOfSets
  }

  def this(set: Set[Set[A]]) {
    this
    for (subset <- set){
      for (int <- subset)
        add(int)
      if (!subset.isEmpty)
        union(subset.toSeq)
    }
  }


  // remove ?
  override def find(element:A): (Option[A],DisjointSets[A]) =
  {
    if (!elements.contains(element)) {
      (None, this)
    }
    else {
      val (rank, parent)=elements(element) // could throw Exception if not in elements!
      if (parent==element) {
        (Some(element), this)
      }
      else
      {
        val root = onlyFind(parent) // need to traverse twice to
                                    // make it tail recursive!
        (Some(root), setParent(element,root).compress(parent,root))
      }
    }
  }

  @annotation.tailrec
  final def union(l:Iterable[A]): DisjointSets[A] = l match {
    case Seq() => this
    case Seq(x) => this
    case Seq(a,xs @ _*) => union(a,xs.head).union(xs)
    }

  override def union(a:A, b:A): DisjointSets[A] =
  {
    val (leftOpt,compressed1) = find(a)
    val (rightOpt,compressed2) = compressed1.find(b)

    (leftOpt, rightOpt) match {

      case (Some(left), Some(right)) if (left != right) =>

        val (lr,lp) = compressed2.elements(left)
        val (rr,rp) = compressed2.elements(right)
        val maybeIncreased =
          if (lr == rr)   // weighted union
            compressed2.setRank(right,rr+1)
          else
            compressed2

        if (lr <= rr)
          maybeIncreased.setParent(left, right)
        else
          maybeIncreased.setParent(right,left)

      case _ =>

        compressed2

    }
  }

  @annotation.tailrec
  final def onlyFind(element: A): A =
  {
    val (rank, parent)=elements(element)
    if (parent==element)
      element
    else onlyFind(parent)
  }

  @annotation.tailrec
  final def compress(element:A, root: A): DisjointSets[A] =
    if (element==root) this
    else {
      val (rank, parent)=elements(element)
      setParent(element,root).compress(parent,root)
    }

  def setRank(e: A, r:Int): DisjointSets[A] = {
    val parent=elements(e)._2
    elements.update(e,(r,parent))
    this
  }

  def setParent(e: A, p:A): DisjointSets[A] = {
    val rank=elements(e)._1
    elements.update(e, (rank, p))
    this
  }


}
