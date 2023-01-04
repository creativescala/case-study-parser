## Controlling Order of Evaluation

The technique we used to implement recursive parsers is a general technique for handling any potentially unbounded recursive structure: delay building the structure until it is needed.

In this section we will:

- touch on a classic application of this idea;
- discuss how it relates to order of evaluation;
- discuss how it relates to ideas of computation vs data; and
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

As an example we can create an infinite list of ones as follows.

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

Now we can do some fun things. First let's see that a finite portion of the infinite list of ones really is all ones.

```scala mdoc:silent
val ones: InfiniteList[Int] = InfiniteList(1, Delay(ones))
```
```scala mdoc
ones.take(5)
```

It seems that our definition has worked.

We can `map` our infinite list, and see that a finite portion is transformed as we expect.

```scala mdoc
ones.map(_ + 2).take(5)
```

Finally, let's define the infinite list of natural numbers using only `ones` defined above.

```scala mdoc:silent
val naturals: InfiniteList[Int] = 
  InfiniteList(1, Delay(ones.zip(naturals).map{ case (a, b) => a + b }))
```
```scala mdoc
naturals.take(5)
```

I suggest making sure you're comfortable with `naturals` before reading on.


### Order of Evaluation

We've seen how we can implement our `InfiniteList` using call-by-name parameters and further delaying evaluation using a no-argument function. Every time we need the delayed value, we evaluate the no-argument function. This general technique is known as call-by-name evaluation. (Note the subtle difference: this is call-by-name *evaluation*, which is more general than call-by-name *parameters*.)

Call-by-name evaluation is not the only possibility. We could evaluate the delayed value the first time it is referred to, and store the result for future use. If we evaluate the delayed value again we just use the stored value, and so avoid repeating the work at the cost of using a bit more memory. This is known as call-by-value or lazy evaluation.

Call-by-value (also known as eager evaluation) is the default in Scala and has no delayed values.


### Computation and Data

One way of looking at call-by-name versus call-by-value is to consider that we can represent data instead by the program that generates that data or vice versa. This perspective is useful in many different contexts. For example, a cache uses data to avoid computation. Compression uses a program (the decompression algorithm) to avoid data. These ideas lead to [algorithmic information theory](https://en.wikipedia.org/wiki/Algorithmic_information_theory) and [Kolmogorov complexity](https://en.wikipedia.org/wiki/Kolmogorov_complexity).

A different perspective comes from programming language theory, which distinguishes data from codata, and recursion from corecursion. Data is finite, while codata is infinite. `InfiniteList` is an example of codata. We have already know lots about data. For algebraic data we've see that, for example, structural recursion allows us to easily manipulate and transform it. There are similar patterns of corecursion for codata, but these techniques are much less well known.


### Further Reading

I first learned about infinite lists from [SICP](https://mitp-content-server.mit.edu/books/content/sectbyfn/books_pres_0/6515/sicp.zip/full-text/book/book-Z-H-24.html#%_sec_3.5). SICP is a classic, but it is a long book, leans heavily on electrical engineering and mathematical examples, and is rather old at this point. I read SICP at exactly the right time in my life (I had just graduated with a computer engineering degree and I had a regular train ride on which to read it) and it blew my mind. However it's not for everyone.

[PLAI](https://www.plai.org/) covers much of the important bits of SICP in a more modern form, and contains no diversions into electrical engineering.

The `InfiniteList` we defined above is equivalent to [`Stream`](https://www.scala-lang.org/api/current/scala/collection/immutable/Stream.html) in the Scala standard library. It is now deprecated in favour of [`LazyList`](https://www.scala-lang.org/api/current/scala/collection/immutable/LazyList.html). This is because `InfiniteList` / `Stream` always evaluates the head of the list. This makes it easier to implement but causes some problems in practice. The `InfiniteList` / `Stream` implementation is implemented in the "odd" style, while `LazyList` is "even". This terminology comes from [How to add laziness to a strict language without even being odd](https://www.diva-portal.org/smash/get/diva2:413532/FULLTEXT01.pdf), which describes the problem and the solution in more detail.

[WTF is Corecursion](https://edward-huang.com/functional-programming/algorithm/programming/scala/2020/08/02/wtf-is-corecursion/) is a very accessible introduction to corecursion that uses Scala. [Corecursion and coinduction: what they are and how they relate to recursion and induction](https://www.cl.cam.ac.uk/archive/mjcg/plans/Coinduction.html) has much more detail but is still quite readable. [The Under-Appreciated Unfold](https://dl.acm.org/doi/pdf/10.1145/289423.289455) goes into detail of `unfold`, which is the corecursive equivalent of `fold`. Finally, [Reasoning about Codata](https://www.cs.ox.ac.uk/ralf.hinze/publications/CEFP09.pdf) has more theory while remaining somewhat readable without a large amount of specialist background.
