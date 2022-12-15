## Designing Using Type Classes

We're now going to design a good portion of the parser interface by using the common type classes as inspiration. Your tasks are to:

- consider each type class interface and see if it makes sense in the context of parsers;
- implement the methods and type class instance if appropriate; and
- implement tests for any methods you've created.

To get started I'll show you how to do the first one, `Functor`.


### Functor

Functor requires only a `map` method, which for a `Parser` will have the type signature

```scala
Parser[A].map[B](f: A => B): Parser[B]
```

This means that we can take an existing parser that produces output of one type, and turn it into a parser that produces an output of some other type. Is this useful? It turns out to be very useful. Right now we can only create a `Parser[String]`. Adding `map` means that we can turn that `String` into any other type we care about, like an `Int` for example.
 
Having decided that we want `map` we'll now implement it in three stages:

1. creating the `map` method on `Parser`;
2. creating the type class instance in the `Parser` companion object; and
3. implementing tests.

Let's go!

Remember we're using the reification pattern for our implementation, for which we need to work out if `map` is a constructor, combinator, or interpreter. The types tell us: `map` takes in a `Parser` and returns a `Parser` so it is a combinator. This means we reify it.

We create a case class that holds *all* the input to the `map` method (remember there is the hidden `this` parameter!)

```scala
final case class ParserMap[A, B](source: Parser[A], f: A => B) extends Parser[B]
```

and the implementation of `map` simply creates an instance of this data structure.

```scala
def map[B](f: A => B): Parser[B] =
  ParserMap(this, f)
```

Easy! Now on to `parse`. We add a case to the pattern match for `ParserMap`

```scala
case ParserMap(source, f) => ???
```

Our first step is to apply the recursion rule for algebraic data types: when the data is recursive the method is recursive.

```scala
case ParserMap(source, f) =>
  loop(source, index)
```

Our recursive call to `loop` returns a `Result`. `Result` is an algebraic data type so we can use structural recursion to make progress.

```scala
case ParserMap(source, f) =>
  loop(source, index) match {
    case Failure(reason, input, start) => ???
    case Success(result, input, offset) => ???
  }
```

Now we can follow the types to finish up the implementation. In the case of `Failure` there is nothing we can do, as we have no data of type `A` to apply to the function `f`. So we just return the `Failure`.

```scala
case ParserMap(source, f) =>
  loop(source, index) match {
    case Failure(reason, input, start) => Failure(reason, input, start)
    case Success(result, input, offset) => ???
  }
```

In the `Success` case we have a result of type `A` to we apply `f` to it to create a value of type `B` and return a success of that.

```scala
case ParserMap(source, f) =>
  loop(source, index) match {
    case Failure(reason, input, start) => Failure(reason, input, start)
    case Success(result, input, offset) =>
      Success(f(result), input, offset)
  }
```

Done, and everything was created using a systematic and repeatable process.

Now it's on to the type class instance. Remember type classes go in the companion object. Beyond that the implementation is straightforward. We only need to implement `map` and we can do that by calling the `map` method we've just created.

```scala
implicit val parserFunctorInstance: Functor[Parser] =
  new Functor[Parser] {
    def map[A, B](fa: Parser[A])(f: A => B): Parser[B] =
      fa.map(f)
  }
```

One small note: remember that `Applicative` extends `Functor`, and `Monad` extends `Applicative`. So if and when you come to implement type class instances for these types make sure you don't have two implementations for the type classes they extend. In other words, if you implement `Applicative` remove the instance for `Functor`. If you don't do this there will be ambiguity when the compiler looks for a  `Functor` instance.

Finally we have tests. We need to test two conditions:

1. `map` does the expected transform when the underlying parser succeeds; and
2. `map` fails if the underlying parser fails.
 
You might notice that these two conditions are exactly the two cases we implemented in the structural recursion on `Result`. It would be very hard to incorrectly implement `map` but we're going add tests anyway. 

The tests are straight-forward modifications of the existing tests for `Parser.string`. I'm not going to include the source code here because it's quite lengthy for very little content. You can find it in the [repository](https://github.com/creativescala/case-study-parser).

Now it is over to you. For each type class below think about possible uses in the context of `Parser`. If you decide it is useful implement it and add tests.


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

In addition, at lot of methods will be available via the type class instances you have created. Let's now do something interesting with our parser library, which will show the power of what we've done.
