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

package parser.expression

import cats.implicits._
import parser._

object Expression {
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

  val digit: Parser[String] =
    List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).foldLeft(Parser.fail[String]) {
      (accum, elt) =>
        accum.orElse(Parser.string(elt.toString))
    }

  val literal: Parser[Expr] =
    digit.and(digit.oneOrMore).map(str => Expr.literal(str.toInt))

  val variable: Parser[Expr] =
    Parser.string("x").as(Expr.variable("x"))

  val factor: Parser[Expr] = literal.orElse(variable)

  val term: Parser[Expr] =
    (factor, Parser.string("*"), Parser.delay(term))
      .mapN((f, _, t) => f * t)
      .orElse(factor)
}
