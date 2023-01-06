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

import cats._
import cats.implicits._

sealed trait Parser[A] {
  import Parser._

  def map[B](f: A => B): Parser[B] =
    ParserMap(this, f)

  def product[B](that: Parser[B]): Parser[(A, B)] =
    ParserProduct(this, that)

  def flatMap[B](f: A => Parser[B]): Parser[B] =
    ParserFlatMap(this, f)

  def orElse(that: => Parser[A]): Parser[A] =
    ParserOrElse(this, () => that)

  def and(that: Parser[A])(implicit s: Semigroup[A]): Parser[A] =
    ParserAnd(this, that, s)

  def zeroOrMore(implicit m: Monoid[A]): Parser[A] =
    repeatAtLeast(0)

  def oneOrMore(implicit m: Monoid[A]): Parser[A] =
    repeatAtLeast(1)

  def repeatAtLeast(minimum: Int)(implicit m: Monoid[A]): Parser[A] = {
    def loop(count: Int): Parser[A] =
      if (count == 0) ParserRepeat(this, m)
      else ParserAnd(this, loop(count - 1), m)

    loop(minimum)
  }

  def repeatBetween(min: Int, max: Int)(implicit m: Monoid[A]): Parser[A] =
    ParserRepeatBetween(this, min, max, m)

  def parse(input: String): Result[A] = {
    def loop[A](parser: Parser[A], index: Int): Result[A] =
      parser match {
        case ParserMap(source, f) =>
          loop(source, index).map(f)

        case ParserProduct(left, right) =>
          loop(left, index) match {
            case f: Failure => f
            case Success(a, _, offset) =>
              loop(right, offset) match {
                case f: Failure => f
                case Success(b, _, offset) =>
                  Success((a, b), input, offset)
              }
          }

        case ParserFlatMap(source, f) =>
          loop(source, index) match {
            case f: Failure => f
            case Success(result, _, offset) =>
              loop(f(result), offset)
          }

        case ParserOrElse(left, right) =>
          loop(left, index) match {
            case _: Failure    => loop(right(), index)
            case s: Success[_] => s
          }

        case ParserAnd(left, right, m) =>
          loop(left, index) match {
            case f: Failure => f
            case Success(x, _, offset) =>
              loop(right, offset) match {
                case f: Failure => f
                case Success(y, _, offset) =>
                  Success(m.combine(x, y), input, offset)
              }
          }

        case ParserTailRecM(f, a) =>
          loop(f(a), index) match {
            case f: Failure => f
            case Success(result, _, offset) =>
              result match {
                case Left(a)  => loop(ParserTailRecM(f, a), offset)
                case Right(b) => Success(b, input, offset)
              }
          }

        case ParserRepeat(source, m) =>
          def repeatLoop(idx: Int, result: A): (Int, A) =
            loop(source, idx) match {
              case Failure(_, _, start) => (start, result)
              case Success(a, _, offset) =>
                repeatLoop(offset, m.combine(result, a))
            }

          val (offset, a) = repeatLoop(index, m.empty)
          Success(a, input, offset)

        case ParserRepeatBetween(source, min, max, m) =>
          def repeatLoop(count: Int, idx: Int, result: A): Option[(Int, A)] =
            if (count > max) (idx, result).some
            else
              loop(source, idx) match {
                case Failure(_, _, _) =>
                  if (count >= min) (idx, result).some
                  else none
                case Success(a, _, offset) =>
                  repeatLoop(count + 1, offset, m.combine(result, a))
              }

          repeatLoop(0, index, m.empty) match {
            case None =>
              Failure(
                s"Did not match between $min and $max times",
                input,
                index
              )
            case Some((idx, result)) => Success(result, input, idx)
          }

        case ParserChar(value) =>
          if (index >= input.size)
            Failure(
              "Input has ended but was expecting character ${value}",
              input,
              index
            )
          else if (input.charAt(index) == value)
            Success(value, input, index + 1)
          else
            Failure(
              s"Input did not contain character $value at index $index",
              input,
              index
            )

        case ParserString(value) =>
          if (input.startsWith(value, index))
            Success(value, input, index + value.size)
          else
            Failure(
              s"Input did not start with $value at index $index",
              input,
              index
            )

        case ParserPure(value) => Success(value, input, index)

        case ParserFail() => Failure("This parser always fails", input, index)

        case ParserSucceed(m) => Success(m.empty, input, index)

        case ParserDelay(p) => loop(p(), index)
      }

    loop(this, 0)
  }
}
object Parser {
  def char(value: Char): Parser[Char] = ParserChar(value)
  def string(value: String): Parser[String] = ParserString(value)
  def pure[A](x: A): Parser[A] = ParserPure(x)
  def fail[A]: Parser[A] = ParserFail()
  def succeed[A](implicit m: Monoid[A]): Parser[A] = ParserSucceed(m)
  def delay[A](parser: => Parser[A]): Parser[A] = ParserDelay(() => parser)
  def charIn(char: Char, chars: Char*): Parser[Char] =
    chars.foldLeft(Parser.char(char)) { (parser, char) =>
      parser.orElse(Parser.char(char))
    }
  def stringIn(string: String, strings: String*): Parser[String] =
    strings.foldLeft(Parser.string(string)) { (parser, string) =>
      parser.orElse(Parser.string(string))
    }

  final case class ParserChar(value: Char) extends Parser[Char]
  final case class ParserString(value: String) extends Parser[String]
  final case class ParserPure[A](value: A) extends Parser[A]
  final case class ParserFail[A]() extends Parser[A]
  final case class ParserSucceed[A](monoid: Monoid[A]) extends Parser[A]

  final case class ParserMap[A, B](source: Parser[A], f: A => B)
      extends Parser[B]
  final case class ParserProduct[A, B](left: Parser[A], right: Parser[B])
      extends Parser[(A, B)]
  final case class ParserFlatMap[A, B](source: Parser[A], f: A => Parser[B])
      extends Parser[B]
  final case class ParserOrElse[A](left: Parser[A], right: () => Parser[A])
      extends Parser[A]
  final case class ParserAnd[A](
      left: Parser[A],
      right: Parser[A],
      semigroup: Semigroup[A]
  ) extends Parser[A]
  final case class ParserRepeat[A](source: Parser[A], monoid: Monoid[A])
      extends Parser[A]
  final case class ParserRepeatBetween[A](
      source: Parser[A],
      min: Int,
      max: Int,
      monoid: Monoid[A]
  ) extends Parser[A]
  final case class ParserTailRecM[A, B](f: A => Parser[Either[A, B]], a: A)
      extends Parser[B]
  final case class ParserDelay[A](parser: () => Parser[A]) extends Parser[A]

  implicit val parserMonadInstance: Monad[Parser] =
    new Monad[Parser] {
      // Override some methods we can implement directly

      override def map[A, B](fa: Parser[A])(f: A => B): Parser[B] =
        fa.map(f)

      override def product[A, B](fa: Parser[A], fb: Parser[B]): Parser[(A, B)] =
        fa.product(fb)

      override def ap[A, B](ff: Parser[A => B])(fa: Parser[A]): Parser[B] =
        ff.product(fa).map { case (f, a) => f(a) }

      def flatMap[A, B](fa: Parser[A])(f: A => Parser[B]): Parser[B] =
        fa.flatMap(f)

      def pure[A](x: A): Parser[A] = Parser.pure(x)

      def tailRecM[A, B](a: A)(f: A => Parser[Either[A, B]]): Parser[B] =
        ParserTailRecM(f, a)
    }
}
