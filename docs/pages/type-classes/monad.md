## Monad

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

Go ahead and implement `flatMap` and tests for it.
