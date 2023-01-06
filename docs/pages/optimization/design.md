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
  def create: Accum
  def append(accum: Accum, in: Input): Accum
  def retrieve(accum: Accum): Output
}
```

