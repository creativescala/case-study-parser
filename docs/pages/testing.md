## Testing Parsers

Our case studies try to be closer to real development. As such, we're now going to write tests for our parser, which allows us to introduce *property-based* or *generative* testing. The core idea is that we generate data test, rather than specifying it by hand. This enables us to cover more test data than we could reasonably write. This requires a change in testing, where we move from testing that we get exactly the expected output to testing that some more general condition holds.

Let's see an example for `Parser.string`. The following conditions should hold:

1. it should parse any `String` that starts with exactly the substring it is expecting; and
2. it should fail to parse any `String` that starts with something else.

Now we can generate random strings and do some simple manipulations to make them match the conditions we identified above. Here's the code:

```scala
property("string succeeds when input starts with expected value") {
  for {
    // We include 0 in the range because parsing an empty String is valid
    expected <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
    suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
    input = expected ++ suffix
    result = Parser.string(expected).parse(input)
  } yield result match {
    case parser.Failure(_, _, _) =>
      fail(s"Parser failed on input $input when it should have failed")
    case parser.Success(_, _, _) => success
  }
}

property("string fails when input does not start with expected value") {
  for {
    // Make sure the prefix is not empty
    prefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
    // Make sure we're not looking for an empty String, because that will match anything
    expected <- Gen.string(Gen.latin1, Range.linear(1, 10)).forAll
    input = prefix ++ expected
    result = Parser.string(expected).parse(input)
  } yield result match {
    case parser.Failure(_, _, _) => success
    case parser.Success(_, _, _) =>
      fail(s"Parser succeeded on input $input when it should have failed")
  }
}
```

This code is using the [HedgehogQA](https://hedgehogqa.github.io/scala-hedgehog/) library and it's [MUnit](https://scalameta.org/munit/) integration. 

The key parts are:

- we write `property` to create a test;
- we use calls to `Gen` to describe how to create our random data, and finish with a call to `forAll`.

As you can see, there's not much too it. The main challenge is coming up with the properties we're testing.
