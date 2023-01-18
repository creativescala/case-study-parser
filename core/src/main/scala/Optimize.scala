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
import parser.Parser._

object Optimize {
  def orElseCharToCharWhere[A](parser: Parser[A]): Parser[A] = {
    def collectCharIn[A](parser: Parser[A]): Option[Set[Char]] =
      parser match {
        case ParserChar(value) => Set(value).some
        case ParserOrElse(left, right) =>
          (collectCharIn(left), collectCharIn(right)).mapN((l, r) => l ++ r)
        case _ => none[Set[Char]]
      }

    def toCharWhere(chars: Set[Char]): Parser[Char] =
      Parser.charWhere(char => chars.contains(char))

    parser match {
      case p: ParserOrElse[a] =>
        val chars = collectCharIn(p)
        chars
          .map(chars => toCharWhere(chars).asInstanceOf[Parser[a]])
          .getOrElse(p)
      case ParserMap(source, f) => ParserMap(orElseCharToCharWhere(source), f)
      case ParserRepeatBetween(source, min, max, monoid) =>
        ParserRepeatBetween(orElseCharToCharWhere(source), min, max, monoid)
      case ParserRepeat(source, monoid) =>
        ParserRepeat(orElseCharToCharWhere(source), monoid)
      case ParserRepeatAccumulator(source, min, accumulator) =>
        ParserRepeatAccumulator(orElseCharToCharWhere(source), min, accumulator)
      case ParserProduct(left, right) =>
        ParserProduct(orElseCharToCharWhere(left), orElseCharToCharWhere(right))
      case ParserAnd(left, right, semigroup) =>
        ParserAnd(
          orElseCharToCharWhere(left),
          orElseCharToCharWhere(right),
          semigroup
        )
      case ParserFlatMap(source, f) =>
        ParserFlatMap(orElseCharToCharWhere(source), f)
      case other => other
    }
  }
}
