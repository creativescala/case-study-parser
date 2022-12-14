## The Simplest Parser

We're going to start by building the most simplest parser library we can: a parser that parses a single fixed string.

We're going to create a type `Parser[A]`, where the type parameter `A` is the type of the result that a successful parse will produce. So, for example, a `Parser[String]` will produce a `String` on a successful parse whereas a `Parser[Album]` will produce an `Album` on a successful parse.

To actually perform the parsing we'll have a method 

```scala
Parser[A].parse(input: String): Result[A]
```

In words, a `Parser[A]` has a method `parse` that accepts a `String` and returns a `Result[A]`. 

What's a `Result[A]`? On a successful parse it will contain a value of type `A` and any remaining unparsed input. Otherwise we'll have an error. We'll get into the details later.

We'll need a way to create `Parsers`. Our very simple constructor will take a `String` as input and return a `Parser[String]` that succeeds if the input starts with exactly that string.

Let's sketch out in Scala what we have so far. We're using a `trait` to represent `Parser` for now, but we'll change that as the design evolves.

```scala
trait Parser[A] {
  def parse(input: String): Result[A]
}
object Parser {
  def string(value: String): Parser[String]
}
```


### The Result Type

Now we can flesh out the implementation. We'll start with `Result`. We previously described it as:

- on a successful parse it will contain a value of type `A` and any remaining unparsed input; otherwise
- we'll have an error. 

This immediately suggests an algebraic data type like

```scala
sealed trait Result[A]
final case class Success[A](result: A, remaining: String) extends Result[A]
final case class Failure[A](reason: String, input: String) extends Result[A]
```

However, for efficiency concerns we'll use a slightly different representation. If our input is `"hello there"` and we parse `"hello"`, the remaining input is `" there"`. To create this `String` we have to copy all the characters from the original input, which takes time proportional to the number of characters remaining. When parsing large input this copying can take a significant amount of time. Instead of copying input we can just record the index of character at which parsing should resume. Character indices start at 0, so in this case parsing should resume at character 5. Now we don't have to do a lot of copying; we can instead just pass around an `Int`.

This gives us the following representation:

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


### Implementing the Parser

Now we have created `Result` we can finish the implementation of `Parser`. Read the code below and I'll then explain the implementation.

```scala mdoc:silent
sealed trait Parser[A] {
  import Parser._

  def parse(input: String): Result[A] = {
    def loop[A](parser: Parser[A], index: Int): Result[A] =
      parser match {
        case ParserString(value) =>
          if (input.startsWith(value, index))
            Success(value, input, index + value.size)
          else
            Failure(
              s"input did not start with $value at index $index",
              input,
              index
            )
      }
    
    loop(this, 0)
  }
}
object Parser {
  final case class ParserString(value: String) extends Parser[String]

  def string(value: String): Parser[String] = ParserString(value)
}
```

Before we get into the implementation let's check that it actually works. Here's an example of succesful and unsuccessful parsing.

```scala mdoc
Parser.string("Hello").parse("Hello there")
Parser.string("Goodbye").parse("Hello there")
```

Now we're satisfied that it works, let's talk about how it works. We can see that `Parser` is an algebraic data type, with `ParserString` as it's single case. `parse` is a structural recursion on `Parser`. As we're dealing with indices we need to know at what index we should start looking at the input. That is why we have the `loop` helper inside `parser`, which implements the actual structural recursion. In the case of `ParserString` we simply see if the `input` starts with the `String` we're looking for. If so, we succeed. Otherwise we fail.

I've described *how* it works, but *why* did I decide to implement the code in this way? There is a pattern behind this code, which we'll look at now.


### Reified Interpreters

The `Parser` is an example of a *reified interpreter*. Let's talk about interpreters first, and then talk about reification.

An interepreter means that there are two parts to our implementation:

1. the part that describes what we want to do, which we call the *program*; and
2. the part that carries out the description, which is the *interpreter*.

In this case `ParserString` is the program. It represents the action we want to carry (look for a particular `String` in the input) and the `String` we should be looking for. The interpreter is in the `parse` method, where we actually go looking for the `String` in the input.

We've seen this pattern in other context, so it should be familiar.

There are a number of different ways to implement an interpreter. The one I've chosen is known as a *deep embedding* or *reification*. Reification means to make concrete something that was abstract. Concretely, in this case we're turning a method (`Parser.string`) into data (`ParserString`).

The general reification pattern works as follows:

1. We divide methods into three classes: constructors, combinators, and interpreters.
2. We reify constructors and combinators into an algebraic data type.
3. We implement interpreters as structural recursions over the algebraic data type we just created.

Constructors, combinators, and interpreters are the three classes of methods that make up what we call an *algebra*. In this case `Parser` is our algebra. We distinguish the three classes of methods as follows:

- A method that goes from something that is not our algebra to our algebra is a *constructor*.
- A *combinator* is any method that takes in our algebra as parameter (often the hidden `this` parameter) and returns our same algebra.
- An *interpreter* takes in our algebra and returns something of a different type.

(Note that sometimes we have interpreter methods that take in and return the algebra type. We'll see one later.)

With this classification we can see that:

1. `Parser.string` is a constructor, because it goes from a `String` to a `Parser[String]`.
2. There are no combinators, yet.
3. `Parser.parse` is an interpreter, because it goes from a `Parser[A]` to a `Result[A]`.

Let's now talk about the specifics of reification. Remember that we reify every constructor and combinator. To do so we create a `final case class` for each method that we're reifying, and:

1. The constructor parameters of the `final case class` are exactly the parameters of the corresponding method. Remember to include the `this` parameter for methods on a `trait` or `class`.
2. The `final case class` extends exactly the return type of the method.

In the case of `Parser.string` there is one parameter and method returns a `Parser[String]`. Hence `ParserString` has a single constructor parameter of type `String` and it extends `Parser[String]`.

Once we have reified to an algebraic data type the interpreter uses the familiar pattern of structural recursion. As is usual in functional programming we have a fancy term for what is ultimately a simple idea.
