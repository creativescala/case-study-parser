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

class ParserOneOfSuite extends HedgehogSuite {
  property("charIn succeeds when input starts with one of expected values") {
    for {
      expected <- Gen.list(Gen.latin1, Range.linear(1, 10)).forAll
      start <- Gen.element(expected.head, expected.tail).forAll
      suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
      input = start +: suffix
      result = Parser.charIn(expected.head, expected.tail: _*).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) =>
        fail(s"Parser failed on input $input when it should have succeeded")
      case parser.Success(_, _, _) => success
    }
  }

  property(
    "charIn fails when input doesn't start with one of expected values"
  ) {
    for {
      prefix <- Gen.upper.forAll
      expected <- Gen.list(Gen.lower, Range.linear(1, 10)).forAll
      start <- Gen.element(expected.head, expected.tail).forAll
      suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
      input = prefix +: start +: suffix
      result = Parser.charIn(expected.head, expected.tail: _*).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) => success
      case parser.Success(_, _, _) =>
        fail(s"Parser succeeded on input $input when it should have failed")
    }
  }

  property(
    "stringIn succeeds when input starts with one of expected values"
  ) {
    for {
      expected <- Gen
        .list(Gen.string(Gen.latin1, Range.linear(1, 10)), Range.linear(1, 10))
        .forAll
      start <- Gen.element(expected.head, expected.tail).forAll
      suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
      input = start ++ suffix
      result = Parser.stringIn(expected.head, expected.tail: _*).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) =>
        fail(s"Parser failed on input $input when it should have succeeded")
      case parser.Success(_, _, _) => success
    }
  }

  property(
    "stringIn fails when input doesn't start with one of expected values"
  ) {
    for {
      prefix <- Gen.upper.forAll
      expected <- Gen
        .list(Gen.string(Gen.lower, Range.linear(1, 10)), Range.linear(1, 10))
        .forAll
      start <- Gen.element(expected.head, expected.tail).forAll
      suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
      input = prefix +: (start ++ suffix)
      result = Parser.stringIn(expected.head, expected.tail: _*).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) => success
      case parser.Success(_, _, _) =>
        fail(s"Parser succeeded on input $input when it should have failed")
    }
  }
}
