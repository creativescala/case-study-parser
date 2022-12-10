# Parsers

We're going to implement a `Parser` type, but before we do that we're going to design the interface we'll create. In previous case studies you've had a lot of guidance. Here we'll be a bit looser; there is some guidance but the end result is ultimately up to you.

We're going to create a type `Parser[A]`, where the type parameter `A` is the type of the result that a successful parse will produce. So, for example, a `Parser[String]` will produce a `String` on a successful parse (not a very informative type), whereas a `Parser[Album]` will produce an `Album` on a successful parse.

To actually perform the parsing we'll have a method 

```scala
Parser[A].parse(input: String): Result[A]
```

In words, a `Parser[A]` has a method `parse` that accepts a `String` and returns a `Result[A]`. 

What's a `Result[A]`? It will contain a value of type `A` if we successfully parsed the `String`, and an error otherwise. We'll get into the details later.

We'll need a way to create `Parsers`. A very simple constructor will take a `String` as input and return a `Parser[String]` that succeeds if the input starts with exactly that string.

Let's sketch out in Scala what we have so far. We're using a `trait` to represent `Parser` for now, but we'll probably change that as the design evolves.

```scala
trait Parser[A] {
  def parse(input: String): Result[A]
}
object Parser {
  def string(value: String): Parser[String]
}
```
