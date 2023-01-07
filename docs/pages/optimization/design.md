## Designing for Performance

In this section we'll see how we can change our parser design to make it more efficient. Our benchmarks suggest two changes:

1. allow filtering of characters based on a predicate function; and
2. allow more efficient ways of accumulating results than concatenating `Strings`.


### Filtering Characters

We've seen that using the `isDigit` method is slightly more efficient than a conditional. It's also much more convenient! This seems like something our library should directly support.

Implement the method

```scala
def charWhere(predicate: Char => Boolean): Parser[Char]
```

and benchmark a new parser built using this instead of `charIn`.

Here are my benchmark results. You can see it's about ten times faster than the original parser! This is definitely an optimization that has paid for itself. 

```
[info] Benchmark                                        Mode  Cnt        Score        Error  Units
[info] RepeatBenchmark.numberParser                    thrpt   25   181549.442 ±   5097.457  ops/s
[info] RepeatBenchmark.numberWhereParser               thrpt   25  1136272.063 ±  42800.845  ops/s
```

You might have noticed that `Parser.char` can be implemented in terms of `Parser.charWhere`, which would simplify our interpreter but perhaps make it slower. Try this change and see if the benchmarks suggest it is harmful for performance.


### Efficiently Accumulating Results

We currently rely on a `Monoid` instance to accumulate results in `oneOrMore` and other combinators. There is no `Monoid` for `Char`, so we first convert to a `String` and then use the instance for that type. Our benchmarks show that this is inefficient. Using a `StringBuffer`, to which we can directly append `Chars`, performs much better. Our challenge is to redesign our API to allow for more efficiency while keeping the generality we currently have.

If we analyse the solution using `StringBuffer` we three operations:

- creating the `StringBuffer`;
- appending a `Char` to the `StringBuffer`; and
- retrieving the `String` from the `StringBuffer` when we've finished.

We can abstract this to:

- a constructor `() => Accum`;
- an appender `(Accum, A) => Accum`; and
- a retrieval `Accum => B`.

(The combiner looks like the type of function we pass to `foldLeft`. This is no coincidence. This type of function, along with some additional structure on `A`, is known as a monoid action.)

We can represent this as a type class

```scala mdoc:silent
trait Accumulator[Input, Accum, Output] {
  def create(): Accum
  def append(accum: Accum, in: Input): Accum
  def retrieve(accum: Accum): Output
}
```

and implement instances of it.

```scala mdoc:silent
object Accumulator {
  implicit val charStringBuilderStringInstance: Accumulator[Char, StringBuilder, String] =
    new Accumulator[Char, StringBuilder, String] {
      def create(): StringBuilder = new StringBuilder()
      def append(accum: StringBuilder, in: Char): StringBuilder =
        accum.append(in)
      def retrieve(accum: StringBuilder): String = accum.result()
    }
}
```

Now we can implement an alternative to `repeatAtLeast`, let's call it `repeatAtLeastA`, that accepts an `Accumulator` instead of `Monoid`. Or can we? If you try this (and you should) you might run into an issue with the middle `Accum` type parameter. Where should it come from? It's not really something relevant to the end user. They know the type of the input and the output, but the accumulator type is an implementation detail. The follow design, using a type member instead of a type parameter, is better.

```scala mdoc:reset:silent
trait Accumulator[Input, Output] {
  type Accum

  def create(): Accum
  def append(accum: Accum, in: Input): Accum
  def retrieve(accum: Accum): Output
}
```

Now we can implement a method

```scala
def repeatAtLeastA[Out](minimum: Int)(implicit
    accum: Accumulator[A, Out]
): Parser[Out]
```

which the user can call like `digit.repeatAtLeastA[String](1)` to produce `String` output. 

I'm not in love with the method name, but let's get benchmark results before committing to this new approach. Go ahead and implement this, and benchmark it.

I implemented two benchmarks: a parser using `charIn` and `repeatAtLeastA`, and a parser using `charWhere` and `repeatAtLeastA`. Full benchmark results are below, again ordered from slowest to fastest.

```
[info] Benchmark                                        Mode  Cnt        Score        Error  Units
[info] RepeatBenchmark.numberParser                    thrpt   25   179171.342 ±   6382.610  ops/s
[info] RepeatBenchmark.numberAccumulatorParser         thrpt   25   219762.072 ±  16873.527  ops/s
[info] RepeatBenchmark.numberWhereParser               thrpt   25  1119387.110 ±  48160.009  ops/s
[info] RepeatBenchmark.numberPatternLoop               thrpt   25  3058160.571 ± 122305.672  ops/s
[info] RepeatBenchmark.numberWhereAccumulatorParser    thrpt   25  3207455.981 ±   7432.588  ops/s
[info] RepeatBenchmark.numberCharacterClassLoop        thrpt   25  3375930.599 ±  26563.046  ops/s
[info] RepeatBenchmark.numberPatternStringBuilderLoop  thrpt   25  6799097.558 ±  17249.332  ops/s
```

You can see that adding the `Accumulator` brings another significant performance gain. In some cases the library is now faster than code that doesn't use parser combinators! 

There is another interesting issue shown by the benchmark results. Notice that `numberAccumulatorParser` is only a small fraction faster than `numberParser`, while `numberWhereAccumulatorParser` is about three times faster than `numberWhereParser`. This is a general issue with benchmarking and optimization: performance is *not composable*. The performance of system A and system B combined is not simply the sum of the performance of the two systems alone. Systems nearly always interact when it comes to performance.

Names

Monoid vs Accumulator
