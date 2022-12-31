## Recursive Expressions

```scala mdoc:invisible:reset-object
sealed trait Expression {
  import Expression._

  def +(that: Expression): Expression =
    Add(this, that)

  def *(that: Expression): Expression =
    Multiply(this, that)
}
object Expression {
  def literal(value: Int): Expression = Literal(value)
  def variable(value: String): Expression = Variable(value)

  final case class Literal(value: Int) extends Expression
  final case class Variable(value: String) extends Expression
  final case class Add(left: Expression, right: Expression) extends Expression
  final case class Multiply(left: Expression, right: Expression) extends Expression
}
```

In the previous section we looked at expressions limited to a single operator with two arguments. We're now going to extend that to handle expressions with an arbitrary number of operators.

We can try writing such a parser right now. We'll start by defining the basic parsers using the methods I added at the end of the previous section.

```scala mdoc:silent
import cats.implicits._
import parser._

val alphabetic: Parser[Char] =
  Parser.oneCharOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                   'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z')
                   
val variable: Parser[Expression] = 
  alphabetic.map(_.toString).oneOrMore.map(str => Expression.variable(str))

val numeric: Parser[Char] =
  Parser.oneCharOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
  
val number: Parser[Expression] =
  numeric.map(_.toString).oneOrMore.map(str => Expression.literal(str.toInt))
  
val whitespace = Parser.oneCharOf(' ', '\t', '\n').void.zeroOrMore
```

Now we can attempt to define parser for expressions with arbitrary expressions. To start with, let's just consider addition.

```scala mdoc:silent
val factor: Parser[Expression] = variable.orElse(number)

val plus: Parser[Char] = whitespace *> Parser.char('+') <* whitespace

val addition: Parser[Expression] =
  (factor, plus, addition).mapN((left, _, right) => left + right).orElse(factor)
```

It seems to have worked. Let's give it a whirl.

```scala mdoc:crash
addition.parse("1 + 2")
```

What the junk?! Our interpreter failed with a pattern match against `null`. How is this possible? Where did this `null` come from?

Let's see an simplified example showing the same behaviour we're seeing in our parser.

```scala mdoc
val a: String = {
  println(s"a is $a")
  "Hello"
}
```

Here we bind the name `a` to a `String`. In the right-hand side expression, which computes the value of `a`, we also refer to `a` in the `println` expression. We see that this is possible, but the value of `a` is `null` at the time when we're computing the value of `a`. Once `a` is defined we get the value we expected, `"Hello"`, when we refer to it.

This explains that issue we saw with our parser. The definition of `addition` refers to itself, and therefore we end up with a `null` value inside the parser we build.

Let's try another approach. Instead of creating a value, we'll create a method that, when called, returns the parser. The parser will refer to the method but that's ok, because the method will already have been defined by the time it is called.

```scala mdoc:silent
def addition2: Parser[Expression] =
  (factor, plus, addition2).mapN((left, _, right) => left + right).orElse(factor)
```

Seems to work, so let's test it.

```scala
addition2
// java.lang.StackOverflowError
// 	at repl.MdocSession$App0$.addition2(recursion.md:98)
// 	at repl.MdocSession$App0$.addition2(recursion.md:98)
// 	at repl.MdocSession$App0$.addition2(recursion.md:98)
// and so on...
```

We get a stack overflow as soon as we reference `addition2`, before we attempt to parse anything. It should be reasonably obvious why this is the case: `addition2` is a recursive method with no base case to stop the recursion.

It turns out we can solve this problem with a small addition to our current API. Before we go through this addition, let's talk about how it works. 

We don't know in advance how many operations there will be in an expression. We could be looking for a single operation, as in `1 + 2`, or many more, as in `1 + 2 + 3 + 4 + 5`. We want to construct a parser that can handle an unbounded number of operations, but this seems to require a parser of infinite size. However, any given expression has only a finite number of operations and therefore requires a finite parser. Therefore we can parse arbitrary expressions by constructing the parser as we need it, so the parser is only as big as the expression we are parsing.

That sound goods, but what does it actually mean in practice? We need two parts:

1. a way to *delay* constructing a parser when we're building the parser; and
2. a change to the interpreter that evaluates delayed parsers, building the next incremental step.
  
For the first we add a new constructor with a call-by-name parameter.
  
```scala 
object Parser {
  def delay[A](parser: => Parser[A]): Parser[A] = ???
}
```

As usual we can implement this method using reification. However we must be careful in the implementation that we don't accidentally evaluate the call-by-name parameter. We do this by wrapping the parameter in a no-argument function.

```scala
object Parser {
  final case class ParserDelay[A](parser: () => Parser[A]) extends Parser[A]

  def delay[A](parser: => Parser[A]): Parser[A] = ParserDelay(() => parser)
}
```

In our interpreter we add a case for `ParserDelay` that evaluates the delayed parser.

```scala
case ParserDelay(p) => loop(p(), index)
```

With these components in place we can finally implement a working parser, by simply delaying the recursive part.

```scala mdoc:silent
val addition3: Parser[Expression] =
  (factor, plus, Parser.delay(addition3)).mapN((left, _, right) => left + right).orElse(factor)
```

We can see this works with an arbitrary number of operations.

```scala mdoc
addition3.parse("1 + 2 + 3 + 4 + 5")
```

Now we have addition working we can extend our parser to handle multiplication as well.

```scala mdoc:silent
val multiply: Parser[Char] = whitespace *> Parser.char('*') <* whitespace

val term: Parser[Expression] = 
  (factor, multiply, Parser.delay(expression)).mapN((left, _, right) => left * right).orElse(factor)

val expression: Parser[Expression] = 
  (term, plus, Parser.delay(expression)).mapN((left, _, right) => left + right).orElse(term)
```
```scala mdoc
expression.parse("1 * 2 + 3 * 4")
```
