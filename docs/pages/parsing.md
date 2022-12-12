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

Ok, now what? We can make some progress by considering the most common type classes: functor, applicative, monad, and monoid. Do they make sense in the context of parsers? Let's look at each in turn.


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

We're dealt with applicative. What about monad? This means a method

```scala
Parser[A].flatMap[B](f: A => Parser[B]): Parser[B]
```

What does this mean in the context of parsing? Can you see uses for `flatMap`? Once again, spend some time on this before reading on.




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
