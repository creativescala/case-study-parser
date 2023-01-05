# Benchmarks and Optimization

In this section we'll benchmark our parser combinator library, and use our benchmarks to optimize our implementation. 

Our benchmarks will use [JMH](https://github.com/openjdk/jmh), which we'll interface with via [sbt-jmh](https://github.com/sbt/sbt-jmh). JMH is standard for benchmarking on the JVM, but it's not my favourite project. To the best of my knowledge the only official documentation for JMH is in the comments in the [JMH samples](https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples) and the [JMH Javadoc](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/index.html). It's not a great state of affairs. 

To get started I've created a benchmark, which you'll find in `benchmark/src/main/scala`. You can use it as an example when you come to creating your own.
