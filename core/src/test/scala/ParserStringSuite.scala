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

import hedgehog._
import hedgehog.munit.HedgehogSuite

class ParserStringSuite extends HedgehogSuite {
  property("string parses all input when input is exactly expected value") {
    for {
      string <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
      result = Parser.string(string).parse(string)
    } yield assertEquals(
      result,
      parser.Result.success(string, string, string.size)
    )
  }

  property("string succeeds when input starts with expected value") {
    val expected = "$%x)"
    for {
      suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
      input = expected ++ suffix
      result = Parser.string(expected).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) =>
        fail(s"Parser failed on input $input when it should have failed")
      case parser.Success(_, _, _) => success
    }
  }

  property("string fails when expected value not found at start of input") {
    val expected = "$%x)"
    val salt = "abc" // This ensures the prefix is not empty
    for {
      prefix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
      input = salt ++ prefix ++ expected
      result = Parser.string(expected).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) => success
      case parser.Success(_, _, _) =>
        fail(s"Parser succeeded on input $input when it should have failed")
    }
  }
}
