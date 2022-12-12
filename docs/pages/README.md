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

To get started you should *fork this repository*. To do this, click the "fork" button at the top right of the Github user interface. The means you'll create your own personal copy of the repository, which is important for the "continuous integration" part of the case study.
