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

/** Indicates the result of a parse. */
sealed trait Result[A]
object Result {
  def success[A](result: A, input: String, offset: Int): Result[A] =
    Success(result, input, offset)

  def failure[A](reason: String, input: String, start: Int): Result[A] =
    Failure(reason, input, start)
}

/** The parse succeeded.
  *
  *   - result is the parsed value
  *   - input is the input that was parsed
  *   - offset is the index of where any remaining input starts.
  */
final case class Success[A](result: A, input: String, offset: Int)
    extends Result[A]

/** The parse failed.
  *
  *   - reason is a description of why the parser failed
  *   - input is the input that the parser attempted to parse
  *   - start is the index into input of where the parser started from
  */
final case class Failure[A](reason: String, input: String, start: Int)
    extends Result[A]
