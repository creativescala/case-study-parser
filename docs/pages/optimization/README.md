# Benchmarks and Optimization

In this section we'll benchmark our parser combinator library, and use our benchmarks to optimize our implementation and inform our API design. Recall there are three main problem solving strategies:

1. reasoning from models;
2. reasoning from data; and
3. appeals to authority.

In previous sections our main tool has been reasoning from models: we've being using reificiation, following types, and so on, which all come from formal models of code. We've mixed in a little bit of API design driven from experience using our library and from existing work on parsers, corresponding to reasoning from data and appeals to authority respectively. 

In this section we'll work much more with reasoning from data, using our benchmarks to drive development. There is still an element of reasoning from models here, as the choice of benchmarks to run and optimizations to attempt is driven from a model of code performance.

Our benchmarks will use [JMH](https://github.com/openjdk/jmh), which we'll interface with via [sbt-jmh](https://github.com/sbt/sbt-jmh). JMH is standard for benchmarking on the JVM, but it's not my favourite project. To the best of my knowledge the only official documentation for JMH is in the comments in the [JMH samples](https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples) and the [JMH Javadoc](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/index.html). It's not a great state of affairs. 

Here's a quick rundown of how JMH works. Most of JMH is controlled by annotations, which are expressions in code that start with `@`. We've already seen the `@tailrec` annotation. A method that we want to use a benchmark requires a `@Benchmark` annotation. Such methods must be declared in a class with no construtor arguments. Being a Java framework, JMH doesn't know about Scala's object literals. If the benchmark method relies on any fields or other values outside the method, these values must be annotated with `@State`. The easiest way to handle this is to annotate the entire class. That's enough to get started. You can find more from the resources above or by reading the benchmark code in this project.
