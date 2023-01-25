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
class RecursionBenchmark {

  val as: Parser[Unit] =
    Parser.char('a').map(_ => ()).and(Parser.delay(as)).orElse(Parser.succeed)

  // val asMemo: Parser[Unit] =
  //   Parser
  //     .char('a')
  //     .map(_ => ())
  //     .and(Parser.memoize(asMemo))
  //     .orElse(Parser.succeed)

  val input =
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

  @Benchmark
  def asParser(): Unit = {
    as.parse(input)
    ()
  }

  // @Benchmark
  // def asMemoParser(): Unit = {
  //   asMemo.parse(input)
  //   ()
  // }
}
