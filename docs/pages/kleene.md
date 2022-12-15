## The Kleene Star

There is one more method we need to implement to have a usable parser combinator library: the [*Kleene star*](https://en.wikipedia.org/wiki/Kleene_star). The what?!? Ok, like most things we've seen it's a complex name for a simple idea: a method that repeats a parser zero or more times.

Imagine we have a parser that parses a single digit.

```scala
val digit: Parser[String] = 
  List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    .fold(Parser.fail[String]){ (accum, digit) =>
      accum.orElse(Parser.string(digit.toString))
    }
```

A number is one or more digits. We don't currently have any way to write this.
