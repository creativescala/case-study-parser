## Monoid

As our next type class, let's consider our last type class, monoid. Remember that a monid requires two things:

1. a combining method; and
2. an identity.

In the context of our `Parser` type, the combine method would have type

```scala
Parser[A].combine(that: Parser[A]): Parser[A]
```

and the identity is an element of type `Parser[A]`.

Can you think of applications of this type of operation for a parser? It may help to think of other monoids, such as those defined on sets and booleans, and consider how they would translate to a parser. If you can come up with a meaningful combine, what is it's identity?

Spend some time thinking about this before reading on.

Monoid is interesting because there at least two choices that make sense for a parser:

1. The combine operation chooses between two parsers, known as alternation. If the first doesn't successfully parse the input we try again with the second input. This is the parser equivalent of the logical or operation, and the union of sets. The identity is the parser that always fails, regardless of it's input.

2. The combine operation combines the results of two parsers. We parse the input with the first parser, parse the remaining input with the second parser, and then combine the output of both parsers if they were both successful. This requires we have a monoid for `A` to combine the output. This is the equivalent of the logical and, and the intersection of sets. The identity is the parser that always succeeds, producing the identity element for `A`.

The first variant of combine, choosing between parsers, is essential. The second is useful but not so important. We have enough work already so we'll skip it for now.

Alternation is conventionally known as `orElse` in Scala, and uses a call-by-name parameter. 

```scala
Parser[A].orElse(that: => Parser[A]): Parser[A]
```

The call-by-name parameter differs from the definition of `combine` on `Monoid`, but will turn out to be essential for parsers.

With alternation we can write the following `Parser` to check for any digit.

```scala
val digit: Parser[Int] =
  List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    .foldLeft(Parser.fail[Int]){ (accum, digit) =>
      accum.orElse(Parser.string(digit.toString).as(digit)) 
    }
```

Note that `as` is a method we get by implementing the `Functor` instance for `Parser`.

Now it's over to you to implement the `orElse` and the identity, which I called `fail` in the example above, and the tests. Remember to follow the reification pattern and you should find this reasonably straightforward.
