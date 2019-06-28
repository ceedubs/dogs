package cats.collections

import cats.{Order, Show}

/**
 * Represent an inclusive range [x, y] that can be generated by using discrete operations
 */
final case class Range[+A](val start: A, val end: A) {

  /**
   * Subtract a Range from this range.
   * The result will be 0, 1 or 2 ranges
   */
  def -[AA >: A](range: Range[AA])(implicit enum: Discrete[AA], order: Order[AA]): Option[(Range[AA], Option[Range[AA]])] =
    if(order.lteqv(range.start, start)) {
      if(order.lt(range.end, start))
        Some(((this: Range[AA]), None))  // they are completely to the left of us
      else if(order.gteqv(range.end, end))
        // we are completely removed
        None
      else Some((Range(enum.succ(range.end), end), None))
    } else {
      if(order.gt(range.start, end))
        Some((this, None)) // they are completely to the right of us
      else {
        val r1 = Range(start, enum.pred(range.start))
        val r2: Option[Range[AA]] = if(order.lt(range.end, end)) Some(Range(enum.succ(range.end), end)) else None
          Some((r1,r2))
      }
    }

  def +[AA >: A](other: Range[AA])(implicit order: Order[AA], enum: Discrete[AA]): (Range[AA], Option[Range[AA]]) = {
    val (l,r) = if(order.lt(this.start,other.start)) (this,other) else (other,this)

    if(order.gteqv(l.end, r.start) || enum.adj(l.end, r.start))
      (Range(l.start, order.max(l.end,r.end)), None)
    else
      (Range(l.start, l.end), Some(Range(r.start,r.end)))

  }

  def &[AA >: A](other: Range[AA])(implicit order: Order[AA]): Option[Range[AA]] = {
    val start = order.max(this.start, other.start)
    val end = order.min(this.end, other.end)
    if(order.lteqv(start,end)) Some(Range(start,end)) else None
  }

  /**
    * Verify that the passed range is a sub-range
    */
   def contains[AA >: A](range: Range[AA])(implicit order: Order[AA]): Boolean =
    order.lteqv(start, range.start) && order.gteqv(end, range.end)

  /**
    * Return an iterator for the values in the range. The iterator moves from
    * start to end taking into consideration the provided ordering.
    * If (start > end) it uses start's predecessor offered by the Discrete
    * instance, otherwise it uses the start's successor.
    */
  def toIterator[AA >: A](implicit discrete: Discrete[AA], order: Order[AA]): Iterator[AA] =
    new Iterator[AA] {
      private var current: AA = start
      private var reachedEnd: Boolean = false

      override def hasNext: Boolean =
        !reachedEnd

      override def next(): AA =
        if (reachedEnd) throw new NoSuchElementException()
        else {
          val r = current
          // increment current
          if (order.lteqv(r, end)) current = discrete.succ(current)
          else current = discrete.pred(current)
          // when current equals end flip the reachedEnd flag
          if (order.eqv(r, end)) {
            reachedEnd = true
          }
          r
        }
      }

  /**
    * Return all the values in the Range as a List.
    */
  def toList[AA >: A](implicit enum: Discrete[AA], order: Order[AA]): List[AA] =
    toIterator[AA].toList

  /**
    * Returns range [end, start]
    */
  def reverse: Range[A] = Range(end, start)

  /**
    * Verify is x is in range [start, end]
    */
  def contains[AA >: A](x: AA)(implicit A: Order[AA]): Boolean = A.gteqv(x, start) && A.lteqv(x, end)

  /**
    * Apply function f to each element in range [star, end]
    */
   def foreach[AA >: A](f: AA => Unit)(implicit enum: Discrete[AA], order: Order[AA]): Unit = {
    var i: AA = start
    while(order.lteqv(i,end)) {
      f(i)
      i = enum.succ(i)
    }
  }

  def map[B](f: A => B): Range[B] = Range[B](f(start), f(end))

  /**
    * Folds over the elements of the range from left to right; accumulates a value of type B
    * by applying the function f to the current value and the next element.
    */
  def foldLeft[AA >: A, B](s: B, f: (B, AA) => B)(implicit discrete: Discrete[AA], order: Order[AA]): B = {
    var b = s
    foreach[AA] { a =>
      b = f(b,a)
    }
    b
  }

  /**
    * Folds over the elements of the range from right to left; accumulates a value of type B
    * by applying the function f to the current value and the next element.
    */
  def foldRight[AA >: A, B](s: B, f: (AA, B) => B)(implicit discrete: Discrete[AA], order: Order[AA]): B =
    reverse.foldLeft(s, (b: B, a: AA) => f(a, b))(discrete.inverse, Order.reverse(order))
}

object Range {
  implicit def rangeShowable[A](implicit s: Show[A]): Show[Range[A]] = new Show[Range[A]] {
    override def show(f: Range[A]): String = {
      val (a, b) = (s.show(f.start), s.show(f.end))
      s"[$a, $b]"
    }
  }

}
