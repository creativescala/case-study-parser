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

class ParserCharSuite extends HedgehogSuite {
  property("char succeeds when input starts with expected value") {
    for {
      expected <- Gen.ascii.forAll
      // We include 0 in the range because parsing an empty Char is valid
      suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
      input = expected +: suffix
      result = Parser.char(expected).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) =>
        fail(s"Parser failed on input $input when it should have succeeded")
      case parser.Success(_, _, _) => success
    }
  }

  property("char fails when input does not start with expected value") {
    for {
      // Make sure the prefix is not empty
      prefix <- Gen.string(Gen.lower, Range.linear(1, 10)).forAll
      expected <- Gen.upper.forAll
      input = prefix :+ expected
      result = Parser.char(expected).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) => success
      case parser.Success(_, _, _) =>
        fail(s"Parser succeeded on input $input when it should have failed")
    }
  }

  test("char doesn't crash on empty string") {
    Parser.char('a').parse("") match {
      case parser.Failure(_, _, _) => success
      case parser.Success(_, _, _) =>
        fail(s"Parser succeeded on empty input when it should have failed")
    }
  }
}
