/*
 * Copyright 2022 Creative Scala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package parser

import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit.SECONDS
import scala.collection.mutable.StringBuilder

/*
 * This benchmarks measures a very common case: collecting all consecutive
 * characters that meet some condition. In the benchmarks below we collect all
 * consecutive digits, as we might do if parsing an integer.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
class RepeatBenchmark {

  val digit: Parser[Char] =
    Parser.charIn('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

  val digitWhere: Parser[Char] = Parser.charWhere(_.isDigit)

  val number: Parser[String] =
    digit.map(_.toString).oneOrMore

  val numberWhere: Parser[String] =
    digitWhere.map(_.toString).oneOrMore

  val numberAccumulator: Parser[String] =
    digit.repeatAtLeastA[String](1)

  val numberWhereAccumulator: Parser[String] =
    digitWhere.repeatAtLeastA[String](1)

  val input = "123456789012345678901234567890"

  /* Use our parser combinator */
  @Benchmark
  def numberParser(): Unit = {
    number.parse(input)
    ()
  }

  @Benchmark
  def numberWhereParser(): Unit = {
    numberWhere.parse(input)
    ()
  }

  @Benchmark
  def numberAccumulatorParser(): Unit = {
    numberAccumulator.parse(input)
    ()
  }

  @Benchmark
  def numberWhereAccumulatorParser(): Unit = {
    numberWhereAccumulator.parse(input)
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
