## Kleene Star

There is one more method we need to implement to have a usable parser combinator library: the [*Kleene star*](https://en.wikipedia.org/wiki/Kleene_star). The what?!? Ok, like most things we've seen it's a complex name for a simple idea: a method that repeats a parser zero or more times.

We've seen how to write a parser that parses a single digit.

```scala
val digit: Parser[Int] = 
  List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    .fold(Parser.fail[String]){ (accum, digit) =>
      accum.orElse(Parser.string(digit.toString).as(digit))
    }
```

A number is one or more digits. We don't currently have any way to write this. The Kleene star allows us to do this. With the Kleene star we can write

```scala
val number: Parser[Int] = digit.and(digit.repeat)
```

The keen-eyed readers will have noticed I've used *two* new methods here: `and` and `repeat`. So what's going on here? Here's the lowdown:

1. `and` is the other monoid operation we discussed earlier, that combines two parsers; and
2. `repeat` is the Kleene star, that repeats a parser zero or more times.

If you think about it, you'll see that the Kleene star is a generalization of `and`.

The rules for `and` are:

- if either parser fails, the combination fails; otherwise
- combine the successful results using the semigroup for `A`.

For `repeat`, the Kleene star, the rules are:

- if no input matches the parser, we succeed with the monoid identity; otherwise
- we use the monoid combine to combine all the results that are successfully parsed.

Go ahead and implement these methods, and tests for them.
