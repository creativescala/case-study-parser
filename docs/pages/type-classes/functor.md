## Functor

Functor requires only a `map` method, which for a `Parser` will have the type signature

```scala
Parser[A].map[B](f: A => B): Parser[B]
```

This means that we can take an existing parser that produces output of one type, and turn it into a parser that produces an output of some other type. Is this useful? It turns out to be very useful. Right now we can only create a `Parser[String]`. Adding `map` means that we can turn that `String` into any other type we care about, like an `Int` for example. Here's an example.

```scala mdoc:invisible
import parser._
```
```scala mdoc:silent
val one: Parser[Int] = Parser.string("1").map(_.toInt)
```
```scala mdoc
one.parse("1")
```
 
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
