## Conclusions

At this point you should have quite a rich interface that looks something like the below:

```scala
sealed trait Parser[A] {
  def map[B](f: A => B): Parser[B]
  def product[B](that: Parser[B]): Parser[(A, B)]
  def flatMap[B](f: A => Parser[B]): Parser[B]
  def orElse(that: => Parser[A]): Parser[A]
  def and(that: Parser[A])(implicit m: Semigroup[A]): Parser[A]
  def repeat(implicit m: Monoid[A]): Parser[A]

  def parse(input: String): Result[A]
}
object Parser {
  def string(value: String): Parser[String]
  def pure[A](value: A): Parser[A]
  def fail[A]: Parser[A]
  def pass[A](implicit m: Monoid): Parser[A]
}
```

In addition, at lot of methods will be available via the type class instances you have created. 

There are two big lessons here:

- we can use type classes as inspiration for designing APIs; and
- we should also consider existing knowledge in a field

Let's now do something interesting with our parser library, which will show the power of what we've done.
