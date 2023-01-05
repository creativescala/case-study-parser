package parser

import org.openjdk.jmh.annotations._
import scala.collection.mutable.StringBuilder

/* This benchmarks measure a very common case: collecting a relatively large
 * `String` where we're testing that each character is within a set of
 * characters. */
@State(Scope.Benchmark)
class RepeatBenchmark {

  val digit: Parser[Char] =
    Parser.oneCharOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

  val number: Parser[String] =
    digit.map(_.toString).oneOrMore

  val input = "123456789012345678901234567890"

  /* Use our parser combinator */
  @Benchmark
  def numberParser(): Unit = {
    number.parse(input)
    ()
  }

  /* A fairly direct translation of our parser into what we might write if doing
   * it by hand. This gives us an idea of the overhead of our parser
   * combinators. */
  @Benchmark
  def numberPatternLoop(): Unit = {
    def loop(idx: Int, result: String): String =
      if (idx == input.size) result
      else {
        val ch = input.charAt(idx)
        ch match {
          case '0' => loop(idx + 1, result :+ ch)
          case '1' => loop(idx + 1, result :+ ch)
          case '2' => loop(idx + 1, result :+ ch)
          case '3' => loop(idx + 1, result :+ ch)
          case '4' => loop(idx + 1, result :+ ch)
          case '5' => loop(idx + 1, result :+ ch)
          case '6' => loop(idx + 1, result :+ ch)
          case '7' => loop(idx + 1, result :+ ch)
          case '8' => loop(idx + 1, result :+ ch)
          case '9' => loop(idx + 1, result :+ ch)
          case _   => result
        }
      }

    loop(0, "")
    ()
  }

  /* Test the performance of using a character class (`isDigit`) compared to the pattern match. */
  @Benchmark
  def numberCharacterClassLoop(): Unit = {
    def loop(idx: Int, result: String): String =
      if (idx == input.size) result
      else {
        val ch = input.charAt(idx)
        if (ch.isDigit) loop(idx + 1, result :+ ch)
        else result
      }

    loop(0, "")
    ()
  }

  /* Test performance of using a StringBuilder instead of concatenating Strings. */
  @Benchmark
  def numberPatternStringBuilderLoop(): Unit = {
    def loop(idx: Int, result: StringBuilder): String =
      if (idx == input.size) result.result()
      else {
        val ch = input.charAt(idx)
        ch match {
          case '0' => loop(idx + 1, result.append(ch))
          case '1' => loop(idx + 1, result.append(ch))
          case '2' => loop(idx + 1, result.append(ch))
          case '3' => loop(idx + 1, result.append(ch))
          case '4' => loop(idx + 1, result.append(ch))
          case '5' => loop(idx + 1, result.append(ch))
          case '6' => loop(idx + 1, result.append(ch))
          case '7' => loop(idx + 1, result.append(ch))
          case '8' => loop(idx + 1, result.append(ch))
          case '9' => loop(idx + 1, result.append(ch))
          case _   => result.result()
        }
      }

    loop(0, new StringBuilder())
    ()
  }
}
