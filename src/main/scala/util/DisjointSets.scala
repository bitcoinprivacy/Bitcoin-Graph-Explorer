package util

import scala.collection.mutable.Map

// A functional, tail recursive implementation of a union-find structure


class DisjointSets[A](val elements: Map[A,(Int,A)]) extends AnyVal {

  def add(element:A): DisjointSets[A] =
    if (elements.contains(element))
      this
    else new DisjointSets[A](elements + (element -> (0,element)))

  // remove ?

  def find(element:A): (A,DisjointSets[A]) =
  {
    val (rank, parent)=elements(element) // could throw Exception if not in elements!
    if (parent==element)
      (element, this)
    else
    {
      val root = onlyFind(parent) // need to traverse twice to
                                  // make it tail recursive!
      (root, setParent(element,root).compress(parent,root))
    }
  }

  @annotation.tailrec
  def onlyFind(element: A): A =
  {
    val (rank, parent)=elements(element)
    if (parent==element)
      element
    else onlyFind(parent)
  }

  @annotation.tailrec
  def compress(element:A, root: A): DisjointSets[A] =
    if (element==root) this
    else {
      val (rank, parent)=elements(element)
      setParent(element,root).compress(parent,root)
    }

  def union(a:A, b:A): DisjointSets[A] =
    {
    val (left,compressed1) = find(a)
    val (right,compressed2) = compressed1.find(b)
    if (left == right) compressed2
    else {
      val (lr,lp) = compressed2.elements(left)
      val (rr,rp) = compressed2.elements(right)
      val maybeIncreased = if (lr == rr)   // weighted union
        compressed2.setRank(right,rr+1)
      else compressed2

      if (lr <= rr)
        maybeIncreased.setParent(left, right)
      else maybeIncreased.setParent(right,left)
    }

    }

  def setRank(e: A, r:Int): DisjointSets[A] = {
    val parent=elements(e)._2
    new DisjointSets(elements.updated(e,(r,parent)))
  }

  def setParent(e: A, p:A): DisjointSets[A] = {
    val rank=elements(e)._1
    new DisjointSets(elements.updated(e,(rank,p)))
  }

  @annotation.tailrec
  def union(l:Iterable[A]): DisjointSets[A] = l match {
    case Seq() => this
    case Seq(x) => this
    case Seq(a,xs @ _*) => union(a,xs.head).union(xs)
    }

}
