## Applicative

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

Go ahead and implement the methods and the tests. When you come to define an `Applicative` instance, you'll need to implement the method `ap`. You can do this in terms of `product` and `map`.
