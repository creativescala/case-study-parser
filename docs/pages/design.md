## Designing Using Type Classes

We're now going to design a good portion of the parser interface by using the common type classes as inspiration. Your tasks are to:

- consider each type class interface and see if it makes sense in the context of parsers;
- implement the methods and type class instance if appropriate; and
- implement tests for any methods you've created.

To get started I'll show you how to do the first one, `Functor`.


### Functor


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


### Wrapping Up

At this point you should have quite a rich interface that looks something like the below:

```scala
sealed trait Parser[A] {
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

In addition, at lot of methods will be available via the type class instances you have created. 

We're almost ready 
Let's now do something interesting with our parser library, which will show the power of what we've done.
