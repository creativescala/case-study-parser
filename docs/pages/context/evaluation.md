## Controlling Order of Evaluation

The technique we used to implement recursive parsers is a general technique for handling any potentially unbounded recursive structure: delay building the structure until it is needed.

In this section we will:

- touch on a classic application of this idea;
- discuss how it relates to order of evaluation; and
- computation vs data
- provide some references where you can learn more.


### Infinite Lists

Building infinite lists is a classic application of delaying evaluation. We already know finite lists. A list of `A` is:

- the empty list; or
- a pair with a head of type `A` and a tail that is a list of `A`.

This is an algebraic data type, and we know systematic techniques to implmement it and write methods using it.

We can change this to an infinite list by delaying the tail. We'll first define a class to hold delayed values. This class has a single method to evaluate the delayed values, known as forcing them.

```scala mdoc:silent:reset-object
final case class Delay[A](delayed: () => A) {
  def force: A = delayed()
}
```

Now we can define an infinite list.

```scala mdoc:silent
sealed trait InfiniteList[A]
final case class Empty[A]() extends InfiniteList[A]
final case class Pair[A](head: A, tail: Delay[InfiniteList[A]]) extends InfiniteList[A]
```

We can define an infinite list of ones as follows.

```scala mdoc:silent
val ones: InfiniteList[Int] = Pair(1, Delay(() => ones))
```

How do we know this is really a lists of ones? Let's redefine our infinite list with some useful methods.

```scala mdoc:reset-object:silent
// This construction allows us to call the apply method on the companion object
// (which has a call-by-name parameter) making it easier to construct delayed
// values.
class Delay[A](delayed: () => A) {
  def force: A = delayed()
}
object Delay {
  def apply[A](value: => A): Delay[A] =
    new Delay(() => value)
}

sealed trait InfiniteList[A] {
  import InfiniteList._
  def ::(value: A): InfiniteList[A] = 
    Pair(value, Delay(this))

  def map[B](f: A => B): InfiniteList[B] =
    this match {
      case Empty()    => Empty()
      case Pair(h, t) => Pair(f(h), Delay(t.force.map(f)))
    }
    
  def zip[B](that: InfiniteList[B]): InfiniteList[(A, B)] =
    (this, that) match {
      case (Pair(h1, t1), Pair(h2, t2)) =>
        Pair((h1, h2), Delay(t1.force.zip(t2.force)))
      case _ => Empty()
    }
    
  def take(count: Int): List[A] =
    if(count <= 0) List.empty
    else {
      this match {
        case Empty()    => List.empty
        case Pair(h, t) => h :: t.force.take(count - 1)
      }
    }
}
object InfiniteList {
  def apply[A](head: A, tail: Delay[InfiniteList[A]]): InfiniteList[A] =
    Pair(head, tail)

  final case class Empty[A]() extends InfiniteList[A]
  final case class Pair[A](head: A, tail: Delay[InfiniteList[A]]) extends InfiniteList[A]
}
```

Now we can do some fun things. First let's see that a finite portion of the infinite list of ones is all ones.

```scala mdoc:silent
val ones: InfiniteList[Int] = InfiniteList(1, Delay(ones))
```
```scala mdoc
ones.take(5)
```

We can `map` our infinte list, and see that a finite portion is transformed as we expect.

```scala mdoc
ones.map(_ + 2).take(5)
```

, and an extension of Scala's call-by-name parameters. Call-by-name parameters are not evaluated until they are referred to, but this is not sufficient for our purposes. Putting the value into a data structure counts as a reference, which would cause evaluation. We've extended call-by-name parameters to delay evaluation to a point of our choosing, by wrapping the parameter in a zero-argument function. With this technique we have complete control over the order of evaluation. What we've implement, in the `Parser` interpreter, is call-by-name evaluation for delayed parsers. (Note the subtle difference: this is call-by-name evaluation, which is more general than call-by-name parameters.) The distinguishing feature of call-by-name evaluation is that we evaluate delayed values every time they are referred to (which is exactly what our interpreter does). In contrast, call-by-need (also known as lazy evaluation) evaluates the delayed value the first time it is referred to, and stores the result for future use. Call-by-value (also known as eager evaluation) is the default in Scala and has no delayed values.


How to add laziness to a strict language without even being odd
Philip Wadler, Walid Taha, David MacQueen, 1998
https://www.diva-portal.org/smash/get/diva2:413532/FULLTEXT01.pdf

Reasoning about Codata
Ralf Hinze, 2009
https://www.cs.ox.ac.uk/ralf.hinze/publications/CEFP09.pdf
