## Parsing Binary Operations

We'll start by parsing expressions that contain a single operator, such as `+`, that has two arguments. For example, we'll parse `4 + 5` or `x * 3`, but not `4 + x * 3`.

This already introduces quite a bit of complexity. In the two examples above we have:

- literal numbers such as `3` and `5`;
- a variable, `x`; and
- the binary operators `+` and `*`.

We'll need to define parsers for each of these components and then compose them together to produce our overall parser.

Let's start with the atomic elements: numbers and variables. We can use the following definitions:

- a literal number is 1 or more digits; and
- a variable is 1 or more alphabetic characters.

This naturally requires us to define digits and alphabetic characters:

- a digit is the character `0`, `1`, `2,`, `3`, `4`, `5`, `6`, `7`, `8`, or `9`; and
- an alphabetic character is one of the lowercase characters between `a` and `z`. 

This is sufficient for us to start writing parsers. I'll get you started.

```scala
val alphabetic: Parser[String] =
  List(`a`, `b`, `c`, `d`, `e`, `f`, `g`, `h`, `i`, `j`, `k`, `l`, `m`,
       `n`, `o`, `p`, `q`, `r`, `s`, `t`, `u`, `v`, `w`, `x`, `y`, `z`)
    .foldLeft(Parser.fail[String]){ (parser, char) =>
      parser.orElse(Parser.string(char.toString))
    }
    
val variable = alphabetic.and(alphabetic.repeat)
```

Now you go ahead and implement parsers for digits and literals.


### Abstract Syntax Trees

Now we can parse very very simple expressions, we should create a data structure that stores the result of our parse. In the compiler literature these data structures are known as *abstract syntax trees* (ASTs). They represent the syntax of the input we parsed, and are usually organized as a tree. In Scala we usually represent abstract syntax trees as algebraic data types (ADTs).

Here's an AST, which is also an ADT, for representing the expressions we described above:

```scala
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
  final case class Variable(value: Int) extends Expression
  final case class Add(left: Expression, right: Expression) extends Expression
  final case class Multiply(left: Expression, right: Expression) extends Expression
}
```

Notice that I've made a bit of effort to make this more pleasant to use. The methods `+` and `*` make it easy to construct compound expressions. The "smart constructors" on the companion object return objects of type `Expression`, which avoids some issues with type inference that you probably won't even realize we've side-stepped.

Also note that this AST is more expressive than the expressions we're trying to parse. We're only parsing expressions that contain a single operator, but our AST can represent arbitrarily nested operators.

Update your parsers so they produce values of type `Expression` on a successful parse. We don't yet have parsers for compound expressions (addition and multiplication) but that's our next step.


### Binary Operations

Our next step is to parse the binary operations `+` and `*`. You might already see how to do this. If so, try it yourself before reading on. If not, I'll show how to implement a parser for addition.

An addition is a literal or variable, followed by the addition sign, followed by another literal or variable. The actually not entirely correct. There can be whitespace separating any of these elements, and our parser needs to account for that. 

Here's my implementation.

```scala
def oneOf(char: Char, chars: Chars*): Parser[String] =
  chars.foldLeft(Parser.string(char.toString)){ (parser, char) =>
    parser.orElse(Parser.string(char.toString)) 
  }
  
val whitespace = oneOf(' ', '\t', '\n')

val factor: Parser[Expression] = literal.orElse(variable)

val addition = (factor, (whitespace *> oneOf('+') <* whitespace), factor)
                 .mapN((l, _, r) => l + r)
```

Notice how defined a utility method, `oneOf`, to make creating parsers easier. I've also used `mapN`, `*>`, and `<*`, which are methods defined by Cats for all Applicatives, to make the code a bit more compact.


### Lessons on API Design
