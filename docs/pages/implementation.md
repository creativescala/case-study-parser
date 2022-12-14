## Implementing Parsers

We're now ready to implement our basic parser API, which we previously designed as the following:

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


### Algebras and Reification

In the API we have the structure of an algebra:

- Constructors creating a `Parser` from another type. These are all methods on the `Parser` companion object.
- Combinators that combine a `Parser` with something (sometimes another `Parser` and sometimes something else) to create a new `Parser`. These are every method on the `Parser` trait that return a `Parser`.
- An interpreter, `parse`, that runs a `Parser` against some input. More abstractly, this gives meaning to the parser, carrying out the instructions encoded in the `Parser` type.

There are two main implementation strategies we can take:

- build a data structure representing combinators and constructors, and the interpreter as a structural recursions over that data structure; or
- represent combinators and constructors as functions that directly carry out the interpreter when passed input.

The former is known as a *deep embedding* or *reification*. The latter is a *shallow embedding* or *Church encoding* (or *̈Boehm–Berarducci encoding* if we're feeling particularly pedantic.)

We've been using the deep embedding implementation strategy so far, and we'll continue using it here. In a later section we will take a brief look at the shallow embedding style to contrast it to the strategy we adopted.


### Implementation

To get started I'm going to show you how to implement the very basics of `Parser`. There are a few implementation details that are specific to parsers that we'll talk about, and which you'll need to understand for your own implementation.

I'm going to implement a single constructor and combinator, and the interpreter for both. I'm choosing `string` and `map` as, respectively, the constructor and combinator. I'm reifying them as an algebraic data type. I'm also putting stub implementations using `???` so I can run the code. By now this implementation strategy should be familiar so I'm just presenting the code below without further explanation.

```scala
sealed trait Parser[A] {
  import Parser._

  def map[B](f: A => B): Parser[B] =
    ParserMap(this, f)

  def product[B](that: Parser[B]): Parser[(A, B)] = ???
  def flatMap[B](f: A => Parser[B]): Parser[B] = ???
  def orElse(that: => Parser[A]): Parser[A] = ???

  def parse(input: String): Result[A] = ???
}
object Parser {
  final case class ParserString(value: String) extends Parser[String]
  final case class ParserMap[A,B](source: Parser[A], f: A => B) extends Parser[B]

  def string(value: String): Parser[String] =
    ParserString(value)

  def pure[A](value: A): Parser[A] = ???
  def fail[A]: Parser[A] = ???
}
```

The next step is to implement the interpreter. This is a structural recursion, and as usual I'm choosing to implement it with pattern matching.

```scala
sealed trait Parser[A] {
  import Parser._

  def map[B](f: A => B): Parser[B] =
    ParserMap(this, f)

  def product[B](that: Parser[B]): Parser[(A, B)] = ???
  def flatMap[B](f: A => Parser[B]): Parser[B] = ???
  def orElse(that: => Parser[A]): Parser[A] = ???

  def parse(input: String): Result[A] =
    this match {
      case ParserString(value) => ???
      case ParserMap(source, f) => ???
    }
}
object Parser {
  final case class ParserString(value: String) extends Parser[String]
  final case class ParserMap[A,B](source: Parser[A], f: A => B) extends Parser[B]

  def string(value: String): Parser[String] =
    ParserString(value)

  def pure[A](value: A): Parser[A] = ???
  def fail[A]: Parser[A] = ???
}
```

Now we get to the novel bit: actually implementing the interpreter. There are two things we need to do here:

1. implement the `Result` type; and
2. make a slight change to the interpreter to account for how parsing will work.

`Result` will have two cases; one for success and one for failure. A successful parse returns the value we constructed from parsing and any remaining input. A failure will return the input we failed to a parse and a reason. Ok, I lied a bit. The previous description is conceptually what we'll do, but for efficiency we'll use an index into the input. So on success we'll return the index of where the remaining input starts, rather than the remaining input itself, and on failure we'll return the index where our failing parser started. This avoids a lot of copying data, which is inefficient.

```scala mdoc:reset-object:silent
/** Indicates the result of a parse. */
sealed trait Result[A]
/** The parse succeeded. 

    - result is the parsed value
    - input is the input that was parsed
    - offset is the index of where any remaining input starts.  */
final case class Success[A](result: A, input: String, offset: Int) extends Result[A]
/** The parse failed.

    - reason is a description of why the parser failed
    - input is the input that the parser attempted to parse
    - start is the index into input of where the parser started from */
final case class Failure[A](reason: String, input: String, start: Int) extends Result[A]
```

Now we have `Result` we'll move on to `parse`. With `Result` using an index into the input we need to pass that index around as we parse. Thus we'll make a change to `parse` to have an inner method that has the index as a parameter. Concretely, this means:

```scala
def parse(input: String): Result[A] = {
  def loop[A](parser: Parser[A], index: Int): Result[A]
    parser match {
      case ParserString(value) => ???
      case ParserMap(source, f) => ???
    }
  loop(this, 0)
}
```

Ok, now we can complete `parse`. For the first case, `ParserString`, we want to see if the input starts with `value` at `index`. The `startsWith` method on `String` that does exactly this.

```scala
def parse(input: String): Result[A] = {
  def loop[A](parser: Parser[A], index: Int): Result[A]
    parser match {
      case ParserString(value) => 
        if (input.startsWith(value, index)) Success(value, input, index + value.size)
        else Failure(s"input did not start with $value at index $index", input, index)

      case ParserMap(source, f) => ???
    }
  loop(this, 0)
}
```

For the second case, `ParserMap`, we can follow the types. Our goal is to produce a `Result[A]`
