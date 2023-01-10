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

import cats.implicits._
import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit.SECONDS

sealed abstract class Expr {
  import Expr._

  def +(that: Expr): Expr = Add(this, that)
  def *(that: Expr): Expr = Mul(this, that)
}
object Expr {
  def literal(value: Int): Expr = Lit(value)
  def variable(value: String): Expr = Var(value)

  final case class Lit(value: Int) extends Expr
  final case class Var(value: String) extends Expr
  final case class Add(left: Expr, right: Expr) extends Expr
  final case class Mul(left: Expr, right: Expr) extends Expr
}

/*
 * This benchmarks measures the cost of continually reevaluating delayed parsers.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
class ExpressionBenchmark {

  val digit: Parser[Char] =
    Parser.charIn('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

  val digitWhere: Parser[Char] = Parser.charWhere(_.isDigit)

  val number: Parser[Int] =
    digit.map(_.toString).oneOrMore.map(_.toInt)

  val numberWhere: Parser[Int] =
    digitWhere.map(_.toString).oneOrMore.map(_.toInt)

  val numberWhereAccumulator: Parser[Int] =
    digitWhere.repeatAtLeastA[String](1).map(_.toInt)

  val add: Parser[Unit] = (Parser.string(" + ")).void
  val mul: Parser[Unit] = (Parser.string(" * ")).void

  val term: Parser[Expr] =
    (number, mul, Parser.delay(term))
      .mapN((f, _, t) => Expr.literal(f) * t)
      .orElse(number.map(Expr.literal _))

  val expr: Parser[Expr] =
    (term, add, Parser.delay(expr))
      .mapN((t, _, e) => t + e)
      .orElse(term)

  val termWhere: Parser[Expr] =
    (numberWhere, mul, Parser.delay(termWhere))
      .mapN((f, _, t) => Expr.literal(f) * t)
      .orElse(numberWhere.map(Expr.literal _))

  val exprWhere: Parser[Expr] =
    (termWhere, add, Parser.delay(exprWhere))
      .mapN((t, _, e) => t + e)
      .orElse(termWhere)

  val termWhereAccumulate: Parser[Expr] =
    (numberWhereAccumulator, mul, Parser.delay(termWhereAccumulate))
      .mapN((f, _, t) => Expr.literal(f) * t)
      .orElse(numberWhereAccumulator.map(Expr.literal _))

  val exprWhereAccumulate: Parser[Expr] =
    (termWhereAccumulate, add, Parser.delay(exprWhereAccumulate))
      .mapN((t, _, e) => t + e)
      .orElse(termWhereAccumulate)

  val termMemo: Parser[Expr] =
    (number, mul, Parser.memoize(termMemo))
      .mapN((f, _, t) => Expr.literal(f) * t)
      .orElse(number.map(Expr.literal _))

  val exprMemo: Parser[Expr] =
    (termMemo, add, Parser.memoize(exprMemo))
      .mapN((t, _, e) => t + e)
      .orElse(termMemo)

  val termWhereMemo: Parser[Expr] =
    (numberWhere, mul, Parser.memoize(termWhereMemo))
      .mapN((f, _, t) => Expr.literal(f) * t)
      .orElse(numberWhere.map(Expr.literal _))

  val exprWhereMemo: Parser[Expr] =
    (termWhereMemo, add, Parser.memoize(exprWhereMemo))
      .mapN((t, _, e) => t + e)
      .orElse(termWhereMemo)

  val termWhereAccumulateMemo: Parser[Expr] =
    (numberWhereAccumulator, mul, Parser.memoize(termWhereAccumulateMemo))
      .mapN((f, _, t) => Expr.literal(f) * t)
      .orElse(numberWhereAccumulator.map(Expr.literal _))

  val exprWhereAccumulateMemo: Parser[Expr] =
    (termWhereAccumulateMemo, add, Parser.memoize(exprWhereAccumulateMemo))
      .mapN((t, _, e) => t + e)
      .orElse(termWhereAccumulateMemo)

  val input =
    "1100 + 24 * 357 * 389 + 8123 * 4998 + 88342 + 23481 * 78934 + 23 * 819 + 241 * 724 * 329904 * 234 * 8322 * 81 + 89083"

  val expected = expr.parse(input)

  assert(expected.isSuccess)

  /* Use our parser combinator */
  @Benchmark
  def exprParser(): Unit = {
    val result = expr.parse(input)
    assert(result == expected)
  }

  @Benchmark
  def exprWhereParser(): Unit = {
    val result = exprWhere.parse(input)
    assert(result == expected)
  }

  @Benchmark
  def exprWhereAccumulateParser(): Unit = {
    val result = exprWhereAccumulate.parse(input)
    assert(result == expected)
  }

  @Benchmark
  def exprMemoizedParser(): Unit = {
    val result = exprMemo.parse(input)
    assert(result == expected)
  }

  @Benchmark
  def exprWhereMemoizedParser(): Unit = {
    val result = exprWhereMemo.parse(input)
    assert(result == expected)
  }

  @Benchmark
  def exprWhereAccumulateMemoizedParser(): Unit = {
    val result = exprWhereAccumulateMemo.parse(input)
    assert(result == expected)
  }
}
