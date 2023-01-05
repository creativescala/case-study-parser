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

Results from my benchmark run are given below. The absolute values don't matter. What is important is the relative differences in performance. I've ordered the results from slowest to fastest.

```
[info] Benchmark                                        Mode  Cnt        Score        Error  Units
[info] RepeatBenchmark.numberParser                    thrpt   25   181950.007 ±   1698.243  ops/s
[info] RepeatBenchmark.numberPatternLoop               thrpt   25  2583254.936 ± 285273.465  ops/s
[info] RepeatBenchmark.numberCharacterClassLoop        thrpt   25  3278595.556 ±  21373.975  ops/s
[info] RepeatBenchmark.numberPatternStringBuilderLoop  thrpt   25  6619869.553 ±  54507.164  ops/s
```
