## Parsers

We're going to implement a `Parser` type, but before we do that we're going to design the interface we'll create. In previous case studies you've had a lot of guidance. Here we'll be a bit looser; there is some guidance but the end result is ultimately up to you.

We're going to create a type `Parser[A]`, where the type parameter `A` is the type of the result that a successful parse will produce. So, for example, a `Parser[String]` will produce a `String` on a successful parse (not a very informative type), whereas a `Parser[Album]` will produce an `Album` on a successful parse.

To actually perform the parsing we'll have a method 

```scala
Parser[A].parse(input: String): Result[A]
```

In words, a `Parser[A]` has a method `parse` that accepts a `String` and returns a `Result[A]`. 

What's a `Result[A]`? On a successful parse it will contain a value of type `A` and any remaining unparsed input. Otherwise we'll have an error. We'll get into the details later.

We'll need a way to create `Parsers`. A very simple constructor will take a `String` as input and return a `Parser[String]` that succeeds if the input starts with exactly that string.

Let's sketch out in Scala what we have so far. We're using a `trait` to represent `Parser` for now, but we'll probably change that as the design evolves.

```scala
trait Parser[A] {
  def parse(input: String): Result[A]
}
object Parser {
  def string(value: String): Parser[String]
}
```

Ok, now what? We can make some progress by considering the most common type classes: monoid, functor, applicative, and monad. Do they make sense in the context of parsers? Let's look at each in turn.


### Monoid

Remember that a monid requires two things:

1. a combining method; and
2. an identity.

In the context of our `Parser` type, the combine method would have type

```scala
Parser[A].combine(that: Parser[A]): Parser[A]
```

Can you think of applications of this type of operation for a parser? It may help to think of other monoids, such as those defined on sets and booleans, and consider how they would translate to a parser.

If you can come up with a meaningful combine, what is it's identity?

Spend some time thinking about this before reading on.


### Functor

Functor requires only a `map`:

```scala
Parser[A].map[B](f: A => B): Parser[B]
```

This is an essential function. 

