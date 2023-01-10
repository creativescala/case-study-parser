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

```scala mdoc:silent
final case class ParserDelay[A](parser: () => Parser[A]) extends Parser[A] {
  lazy val force: Parser[A] = parser()
}
```

Now we should verify this change actually improves performance. Parsing expressions, which we looked at in the [previous section](/expressions/recursion.md), depends on delayed parsers and therefore makes a good benchmark. change. Be careful when benchmarking this change. You may need to change how `orElse` is implemented in addition to `Parser.delay`.

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

A failing experiment is still a useful experiment. It shows that my model of program performance doesn't match reality, and I need to revise my model. To understand where my model needs to be revised would require more detail about how the code runs inside the JVM. A good place to start is with a profiler such as [Visual VM](https://visualvm.github.io/index.html). My suspicion is that optimizations within the JVM turn the two different implementations into equivalent code. To verify this I expect I would need to look at the generated output, which is really getting into the weeds. [This blog post](https://www.beyondjava.net/show-assembly-code-generated-jvm) goes into more detail on how this can be done.


### Rewriting
