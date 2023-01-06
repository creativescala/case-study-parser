## Benchmarking 

One very common parsing operation is to collect all consecutive characters that meet some condition. For example, the parser below collects all consecutive digits, and can be used to parse integers.

```scala
val digit: Parser[Char] =
  Parser.oneCharOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

val number: Parser[String] =
  digit.map(_.toString).oneOrMore
```

I decided to start benchmarking with this common case. I created four benchmarks, which you can find in `benchmarks/src/main/scala/RepeatBenchmark.scala`:

1. `numberParser` measures the baseline performance of the parser combinator library;
2. `numberPatternLoop` measures the same algorithm written without the parser combinator framework, giving an idea of the overhead of combinator library adds;
3. `numberCharacterClassLoop` replaces the explicit conditional with a call to the `isDigit` method on `Char`; and
4. `numberPatternStringBuilderLoop` measures the effect of using a `StringBuilder` instead of concatenating `Strings` in `numberPatternLoop`

You can run the benchmarks using the following `sbt` commands:

- `project benchmarks` to change into the benchmarks projects within sbt; and
- `Jmh / run` to run the benchmarks with the default JMH settings.

The benchmarks will take a few minutes to run, which is good for getting accurate results but not for quick iteration of optimizations. The command `Jmh / run -h` will output the many arguments you can pass to JMH to change how it runs the benchmarks. Using `Jmh / run -i 1 -wi 1 -f 1` will run many fewer iterations, giving results in a few seconds at the risk of more inaccuracy in measurements. In my testing this was accurate enough for the large performance that we're looking for here, though I did verify the results with a longer run once I'd finished an optimization.

Results from my initial benchmark run are below. I've ordered the results from slowest to fastest. The absolute values don't matter; what is important is the relative differences in performance. 

We can see:

- the parser combinator approach is at least an order of magnitude slower than any other approach, suggesting the combinator library adds significant overhead;
- using a `StringBuilder` is about twice as fast as concatenating `Strings`; and
- using the character class method `isDigit` is a slight improvement over the explicit test.

```
[info] Benchmark                                        Mode  Cnt        Score        Error  Units
[info] RepeatBenchmark.numberParser                    thrpt   25   181950.007 ±   1698.243  ops/s
[info] RepeatBenchmark.numberPatternLoop               thrpt   25  2583254.936 ± 285273.465  ops/s
[info] RepeatBenchmark.numberCharacterClassLoop        thrpt   25  3278595.556 ±  21373.975  ops/s
[info] RepeatBenchmark.numberPatternStringBuilderLoop  thrpt   25  6619869.553 ±  54507.164  ops/s
```

We can use these findings to inform API design and internal optimizations.
