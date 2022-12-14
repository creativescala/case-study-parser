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

sealed trait Parser[A] {
  import Parser._

  def parse(input: String): Result[A] = {
    def loop[A](parser: Parser[A], index: Int): Result[A] =
      parser match {
        case ParserString(value) =>
          if (input.startsWith(value, index))
            Success(value, input, index + value.size)
          else
            Failure(
              s"input did not start with $value at index $index",
              input,
              index
            )
      }

    loop(this, 0)
  }
}
object Parser {
  final case class ParserString(value: String) extends Parser[String]

  def string(value: String): Parser[String] = ParserString(value)
}
