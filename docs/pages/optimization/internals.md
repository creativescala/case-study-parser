## Internal Optimizations

In this section we'll look at internal optimizations that are invisible to the end user.


### Memoization

Our current implementation of `delay` re-evaluates the delayed `Parser` every time it is encountered in the interpreter. This is wasteful as the `Parser` will not change between evaluations. We can store the value after we first evaluate it, a technique known as [memoization](https://en.wikipedia.org/wiki/Memoization). 

Scala's `lazy val` makes memoization straightforward. A `lazy val` is like a `val` except the right-hand side expression, which produces the value, is not evaluated until the `lazy val` is first used. The value is then saved and used for subsequent uses. The following example shows these semantics. Note the `println` is not evaluated until the first use, but future uses do not re-evaluate the `println`.

```scala mdoc
lazy val a = { 
  println("Evaluating")
  1
}
a
a
```

(Make sure you understand the difference between the behaviour of `val`, `lazy val`, and `def`. If you're unsure on the difference, replace `lazy val` with `val` and with `def` and see how the output changes.)

With a `lazy val` we can change the reification of `delay`, which in my implmentation is a case class called `ParserDelay`, to memoize the delayed parser. I've shown how to do this below, but try to do it yourself before reading my solution.

```scala
final case class ParserDelay[A](parser: () => Parser[A]) extends Parser[A] {
  lazy val force: Parser[A] = parser()
}
```

Now we should verify this change actually improves performance. Parsing expressions, which we looked at in the [previous section](../expressions/recursion.md), depends on delayed parsers and therefore makes a good benchmark. Be careful when benchmarking this change. You may need to change how `orElse` is implemented in addition to `Parser.delay`.

Below are the results from my experiments. I don't see any consistent improvement from memoization; the memoized and non-memoized parsers are within the error bounds of each other. The results are surprising to me; I was expecting to see more effect from memoization. I tried a lot of different implementations to try to find a consistent performance difference, but was unable to do so.

```
[info] Benchmark                                               Mode  Cnt       Score      Error  Units
[info] ExpressionBenchmark.exprParser                         thrpt   25   30904.951 ±  612.385  ops/s
[info] ExpressionBenchmark.exprMemoizedParser                 thrpt   25   31923.289 ±  520.764  ops/s
[info] ExpressionBenchmark.exprWhereMemoizedParser            thrpt   25  123323.837 ± 3973.923  ops/s
[info] ExpressionBenchmark.exprWhereParser                    thrpt   25  128436.999 ± 3552.719  ops/s
[info] ExpressionBenchmark.exprWhereAccumulateParser          thrpt   25  182851.316 ± 3105.287  ops/s
[info] ExpressionBenchmark.exprWhereAccumulateMemoizedParser  thrpt   25  183370.477 ± 8889.627  ops/s
```

A failing experiment is still a useful experiment. It shows that my model of program performance doesn't match reality, and I need to revise my model. In this case the issue is something relatively mundane. We're still avoiding some work with memoization, but the work is so trivial that it doesn't meaningfully effect performance. When we create a recursive parser, like the one below, we're creating a circular data structure. The when we evaluate the `delay` we're following a reference back into the data structure. The performance difference between evaluating a function to produce this reference, or just directly getting the reference, is very small.

```scala
val term: Parser[Expr] =
  (number, mul, Parser.delay(term))
    .mapN((f, _, t) => Expr.literal(f) * t)
    .orElse(number.map(Expr.literal _))
```

In more complicated situtation we might want more detail about how the code runs inside the JVM. A good place to start is with a profiler such as [Visual VM](https://visualvm.github.io/index.html). This may not give enough detail, in which case we can look at the generated output code. [This blog post](https://www.beyondjava.net/show-assembly-code-generated-jvm) goes into more detail on how this can be done.


### Rewriting

In many cases we can express the same functionality using  `charIn` or `charWhere`. This is a problem when it comes to reasoning about performance. We've seen that `charWhere` can be much more efficient than `charIn`, but its not obvious to the user that this is the case. We'd ideally like equivalent code to have roughly equivalent performance no matter how it's written, rather than requiring the developer to know obscure implementation specific rules.

We can achieve this by rewriting `charIn` to `charWhere`. In fact we can do better, by rewriting any chain of `orElse` and `Parser.char` into `charWhere`. For example, if we see

```scala mdoc:silent
Parser.char('a').orElse(Parser.char('b'))
```
we can rewrite it to

```scala mdoc:silent
Parser.charWhere(x => x == 'a' || x == 'b')
```

This type of optimization, where we rewrite code into a functionally equivalent but more efficient alternative, is the core of what optimizing compilers do.


I want to be able to benchmark this optimization against the current implementation, so I'm going to implement it in a separate object in a separate file. Because we've reified `Parser`, and hence it's represented as an algebraic data type, we can easily do this. 

My implementation of the optimization has three parts:

- given a `Parser`, I walk over the tree of elements looking for an `orElse`;
- once I find an `orElse` I see if it only contains `orElse` and `charIn`; and
- if so, I convert into an equivalent `charWhere`; otherwise I leave it alone.

Have a go at implementing it yourself. If you struggle, my implementation it below. Once you have an implementation run some benchmarks. In my benchmarks, results of which are also below, this transformation results in code that is about 25 times faster!

Here's the implementation:

```scala mdoc:silent
import cats.implicits._
import parser.Parser._

object Optimize {
  def orElseCharToCharWhere[A](parser: Parser[A]): Parser[A] = {
    def collectCharIn[A](parser: Parser[A]): Option[Set[Char]] =
      parser match {
        case ParserChar(value) => Set(value).some
        case ParserOrElse(left, right) =>
          (collectCharIn(left), collectCharIn(right)).mapN((l, r) => l ++ r)
        case _ => none[Set[Char]]
      }

    def toCharWhere(chars: Set[Char]): Parser[Char] =
      Parser.charWhere(char => chars.contains(char))

    parser match {
      case p: ParserOrElse[a] =>
        val chars = collectCharIn(p)
        chars
          .map(chars => toCharWhere(chars).asInstanceOf[Parser[a]])
          .getOrElse(p)
      case ParserMap(source, f) => ParserMap(orElseCharToCharWhere(source), f)
      case ParserRepeatBetween(source, min, max, monoid) =>
        ParserRepeatBetween(orElseCharToCharWhere(source), min, max, monoid)
      case ParserRepeat(source, monoid) =>
        ParserRepeat(orElseCharToCharWhere(source), monoid)
      case ParserRepeatAccumulator(source, min, accumulator) =>
        ParserRepeatAccumulator(orElseCharToCharWhere(source), min, accumulator)
      case ParserProduct(left, right) =>
        ParserProduct(orElseCharToCharWhere(left), orElseCharToCharWhere(right))
      case ParserAnd(left, right, semigroup) =>
        ParserAnd(
          orElseCharToCharWhere(left),
          orElseCharToCharWhere(right),
          semigroup
        )
      case ParserFlatMap(source, f) =>
        ParserFlatMap(orElseCharToCharWhere(source), f)
      case other => other
    }
  }
}
```

Here are the benchmark results:

```
[info] Benchmark                                          Mode  Cnt        Score       Error  Units
[info] ToCharWhereBenchmark.parser                       thrpt   25   104026.788 ±   565.152  ops/s
[info] ToCharWhereBenchmark.parserOrElseCharToCharWhere  thrpt   25  2565950.528 ± 54878.768  ops/s
```
