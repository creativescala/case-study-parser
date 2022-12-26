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
  List('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
       'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z')
    .foldLeft(Parser.fail[String]){ (parser, char) =>
      parser.orElse(Parser.string(char.toString))
    }
    
val variable = alphabetic.and(alphabetic.repeat)
```

Now you go ahead and implement parsers for digits and literals.


### Abstract Syntax Trees

Now we can parse very very simple expressions, we should create a data structure that stores the result of our parse. In the compiler literature these data structures are known as *abstract syntax trees* (ASTs). They represent the syntax of the input we parsed, and are usually organized as a tree. In Scala we usually represent abstract syntax trees as algebraic data types (ADTs).

Here's an AST, which is also an ADT, for representing the expressions we described above:

```scala mdoc:silent:reset-object
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

Notice that I've made a bit of effort to make this more pleasant to use. The methods `+` and `*` make it easy to construct compound expressions. The "smart constructors" on the companion object return objects of type `Expression`, which avoids some issues with type inference that you probably won't even realize we've side-stepped.

Also note that this AST is more expressive than the expressions we're trying to parse. We're only parsing expressions that contain a single operator, but our AST can represent arbitrarily nested operators.

Update your parsers so they produce values of type `Expression` on a successful parse. We don't yet have parsers for compound expressions (addition and multiplication) but that's our next step.


### Binary Operations

Our next step is to parse the binary operations `+` and `*`. You might already see how to do this. If so, try it yourself before reading on. If not, I'll show how to implement a parser for addition. Multiplication is a straightforward modification of it.

An addition is a literal or variable, followed by the addition sign, followed by another literal or variable. The actually not entirely correct. There can be whitespace separating any of these elements, and our parser needs to account for that. 

Here's my implementation.

```scala
def oneOf(char: Char, chars: Chars*): Parser[String] =
  chars.foldLeft(Parser.string(char.toString)){ (parser, char) =>
    parser.orElse(Parser.string(char.toString)) 
  }
  
val whitespace = oneOf(' ', '\t', '\n').repeat

val factor: Parser[Expression] = literal.orElse(variable)

val addition = (factor, (whitespace *> oneOf('+') <* whitespace), factor)
                 .mapN((l, _, r) => l + r)
```

A few points:

- I defined a parser `factor` to refer to a literal or a variable.
- I created a utility method, `oneOf`, to make creating parsers easier.
- I've also used `mapN`, `*>`, and `<*`, which are methods defined by Cats for all applicatives, to make the code a bit more compact. You should be familiar with `mapN`, but `*>` and `<*` may be new to you. The way to read them is as binary operations that only keep the value pointed to by the arrow. So in `whitespace *> oneOf('+') <* whitespace` I only keep the result of parsing the `+` and discard the result of parsing any whitespace. These methods are on the edge for me: they are not so well known that I feel confident using them in all codebases. In this context, though, it's an opportunity for you learn so I've used them.


### Lessons on API Design

Our work so far as exposed some flaws in our current design. Here are the issues I noted:

1. We have to use a `Parser[String]` just to look for a single `Char`. A `String` contains 0 or more characters, while a `Char` is exactly one character. It would be better if could use the more precise type `Char` when we want exactly one `Char`.

2. `oneOf` is the kind of method that should be provided by our parser library, as it's a case that frequently occurs. There are at least two variants we can envisage: one where we specify the characters we're looking for, and one where we specify a predicate that is `true` if the character is one we're looking for.

3. The construction `parser.and(parser.repeat)` is an inconvenient way to express we that we want to repeat a parser one or more times, and is certainly a bit opaque to someone new to the library. This operation is sometimes known as the Kleene plus, and it's very common. We should consider supporting it directly.

Designing using type classes gets us some of the way to a good API, but not all of the way. We still need domain specific methods like the Kleene star. Working on examples is a good way to discover missing methods, as we've done here.

I added the following constructors:

- `Parser.char(value: Char): Parser[Char]`
- `Parser.oneCharOf(char: Char, chars: Char*): Parser[Char]`
- `Parser.oneStringOf(string: String, strings: String*): Parser[String]`

I then reworked the `repeat` combinator:

- I added `repeatAtLeast(minimum: Int)`, which allows the called to specify the minimum number of repeats for a succesful parse;
- I added `zeroOrMore` and `oneOrMore` as special cases of `repeatAtLeast`; and
- I added `repeatBetween(min: Int, max: Int)` to allow specifying a maximum number of repeats.
