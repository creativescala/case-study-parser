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
  (factor, plus, addition).mapN((left, _, right) => left + right)
```

It seems to have worked. Let's give it a whirl.

```scala mdoc:crash
addition.parse("1 + 2")
```

What the junk?! Our interpreter failed with a pattern match against `null`. How is this possible? Where could this `null` possibly have come from?

Let's see an simplified example that shows the behaviour we're seeing.

```scala mdoc
val a: String = {
  println(s"a is $a")
  "Hello"
}
```

Here we bind the name `a` to a `String`. In the right-hand side expression, where we compute the value of `a`, we also refer to `a` in the `println` expression. We see that this is possible, but the value of `a` is `null` at the time when we're computing the value of `a`. Once `a` is defined we get the value we expected, `"Hello"`, when we refer to it.

This explains that issue we saw with our parser. The definition of `addition` refers to itself, and therefore we end up with a `null` value inside the parser we build.

Let's try another approach. Instead of creating a value, we'll create a method that, when called, returns the parser. The parser will refer to the method but that's ok, because the method will already have been defined by the time it is called.

```scala mdoc:silent
def addition2: Parser[Expression] =
  (factor, plus, addition).mapN((left, _, right) => left + right)
```

Seems to work, so let's test it.

```scala mdoc
addition2.parse("1 + 2")
```
