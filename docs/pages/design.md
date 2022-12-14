## Designing Parsers


### Functor

Functor requires only a `map` method, which for a `Parser` will have the type signature

```scala
Parser[A].map[B](f: A => B): Parser[B]
```

This means that we can take an existing parser that produces output of one type, and turn it into a parser that produces an output of some other type. Is this useful? It turns out to be very useful. Right now we can only create a `Parser[String]`. Adding `map` means that we can turn that `String` into any other type we care about, like an `Int` for example.
 
 
### Applicative

Applicative defines `ap` and `pure`, but remember that in Scala we mostly interact with the `product` method defined on `Semigroupal` via `mapN`. So we'll consider `product` and `pure`, which together with `map` are sufficient to create an applicative.

```scala
Parser[A].product[B](that: Parser[B]): Parser[(A, B)]
pure[A](value: A): Parser[A]
```

Can you think of a use for these methods? Give this a bit of time before reading on.

Both methods are useful, though `product` is the more useful of the two. `Product` means we can combine in sequence two or more parsers that produce different types. We started with an example of parsing text fields into the following data structure:

```scala
final case class Album(artist: String, name: String, year: Int)
```

If we have a `Parser[String]` and a `Parser[Int]` we create an `Album` with a combination of `product` and `map`.

```scala
val stringParser: Parser[String] = ???
val intParser: Parser[Int] = ???

stringParser
  .product(stringParser)
  .product(intParser)
  .map{ case ((a, n), y) => Album(a, n, y) }
```

We can more simply write this using `mapN`: 

```scala
(stringParser, stringParser, intParser)
  .mapN((a, n, y) => Album(a, n, y))
```

`Pure` is less useful, but is still nice to have. It constructs a `Parser` that always produces the given value and ignores it's input. It's sometimes useful when we know that if we parse some input then some other output is implied. We can create this other output with `pure`. (No example here because I can't think of one right now!)


### Monad

We've dealt with applicative. What about monad? This means a method

```scala
Parser[A].flatMap[B](f: A => Parser[B]): Parser[B]
```

What does this mean in the context of parsing? Can you see uses for `flatMap`? Once again, spend some time on this before reading on.

`FlatMap`allows us to switch to a parser based on the output of another parser. This is done by the function `f` passed to `flatMap`. It takes the output of the first parser, and can use that output to choose a parser to parse the rest of the input. For example, we could use `flatMap` to parse a file where one field tells us the format of the next field. A file like

```
number: 100
string: hello
```

could be parsed with

```scala
val fieldParser: Parser[String] = ??? // parses <type>: and returns <type>
val intParser: Parser[Int] = ???
val stringParser = Parser[String] = ???

fieldParser.flatMap(result =>
  result match {
    case "number" => intParser
    case "string" => stringParser
  }
)
```

Adding `flatMap` has some drawbacks, which we'll see later on, but for now we'll add it in.


### Monoid

Finally, let's consider our last type class, monoid. Remember that a monid requires two things:

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

As we have two useful monoids for parsers, when we come to implement them we should define methods directly on `Parser` rather than implementing them via a type class. Why? Because type classes work best when there is one canonical implementation for a given type. Switching between implementations requires us to explicitly pass the type class instance, or otherwise bring it into scope, which defeats much of the point of type classes. 

Alternation is conventionally known as `orElse` in Scala, and uses a call-by-name parameter. 

```scala
Parser[A].orElse(that: => Parser[A]): Parser[A]
```

The call-by-name parameter will turn out to be essential for parsers, and differs for the definition of `combine` on `Monoid`, which is another good reason to implement the method directly.


### Our Final Initial Interface

At this point we've developed quite a rich interface:

```scala
trait Parser[A] {
  def map[B](f: A => B): Parser[B]
  def product[B](that: Parser[B]): Parser[(A, B)]
  def flatMap[B](f: A => Parser[B]): Parser[B]
  def orElse(that: => Parser[A]): Parser[A]

  def parse(input: String): Result[A]
}
object Parser {
  def string(value: String): Parser[String]
  def pure[A](value: A): Parser[A]
  def fail[A]: Parser[A]
}
```

There are more methods we'll eventually need, but this is more than enough to get us started. Let's move on to implementation and get a complete system working.
