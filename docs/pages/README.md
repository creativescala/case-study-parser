# Case Study: Parsing

This case study is about parsing, which is the process of building structure from unstructured input. For example, if we have a file with lines like

```
David Bowie      | The Rise and Fall of Ziggy Stardust | 1972
Autechre         | LP5                                 | 1998
Tadpole          | The Buddhafinger                    | 2000
Julianna Barwick | Healing Is a Miracle                | 2020
```

we might want to turn each line into a

``` scala
final case class Album(artist: String, name: String, year: Int)
```

To do this we can create a parser for the data format, which in this case is fields separated by `|`.

In this case study we'll:

- build a *parser combinator library*, that allows us to easily construct parsers in a compositional way;
- use our library to parse some example data;
- consider optimizations and measure performance improvements; and finally
- put our implementation in the context of wider work on parser combinators.

Specific programming techniques we'll look at include:

- old friends algebraic data types and structural recursion;
- reification and interpreters;
- monoids, applicatives, and, possibly, monads;
- benchmarking and optimizations;
- generative testing; and
- continuous integration.


## Getting Started

To get started you should *fork this [repository](https://github.com/creativescala/case-study-parser)*. To do this, click the "fork" button at the top right of the Github user interface. The means you'll create your own personal copy of the repository, which is important for the "continuous integration" part of the case study.


## Next Steps

Our next step is to create our parser combinator library. We're going to do this in three broad sections:

1. I'll start by showing you the implementation for a very minimal library. This will demonstrate two things: using type classes to guide design, and using reification as an implementation strategy.
2. I'll then ask you to design more of the interface, using common type classes as inspiration, and then use the same implementation technique of reification to develop the library further.
3. We'll then discuss some parser specific methods, and incorporate those into the library.
