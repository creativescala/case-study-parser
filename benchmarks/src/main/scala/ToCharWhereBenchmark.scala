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

/*
 * This benchmark measures the code of delaying and forcing parsers.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
class ToCharWhereBenchmark {

  val alphabetic: Parser[Char] =
    Parser.charIn('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
      'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z')

  // val word = alphabetic.repeatAtLeastA(1)

  // val wordOrElseCharToCharWhere = Optimize.orElseCharToCharWhere(word)

  val input =
    "abcdefghijklmnopqrstuvwxyz"

  // val expected = word.parse(input)

  // @Benchmark
  // def parser(): Unit = {
  //   val result = word.parse(input)
  //   assert(result == expected)
  // }

  // @Benchmark
  // def parserOrElseCharToCharWhere(): Unit = {
  //   val result = wordOrElseCharToCharWhere.parse(input)
  //   assert(result == expected)
  // }
}
