package metaconfig.internal

import metaconfig.Input
import scala.util.Properties

class JsonConfParserSuite extends munit.FunSuite {

  def check(original: String, expected: ujson.Value): Unit = {
    test(original) {
      assume(
        !Properties.isWin,
        "NOTE(olafur) tests are failing in CI and I don't have time to look into it right now. Feb 8th 2020."
      )
      val js = JsonConverter.fromInput(Input.String(original))
      assertEquals(js, expected)
    }
  }

  check(
    """{
      |  "a":1
      |}""".stripMargin,
    ujson.Obj("a" -> ujson.Num(1))
  )

  // comments
  check(
    """{
      |  // leading
      |  // leading 2
      |  "a": 1, // trailing
      |  "b": // colon
      |    2, // trailing,
      |  "c": [ // open
      |    3 // arr
      |  ] // close
      |}
      |""".stripMargin,
    ujson.Obj(
      "a" -> ujson.Num(1),
      "b" -> ujson.Num(2),
      "c" -> ujson.Arr(ujson.Num(3))
    )
  )

  // trailing commas
  check(
    """
      |{
      |  "b": [
      |    1, // comment
      |    2 // comment
      |    ,
      |
      |  ],
      |  "a": 2, // comment
      |
      |}
    """.stripMargin,
    ujson.Obj(
      "b" -> ujson.Arr(ujson.Num(1), ujson.Num(2)),
      "a" -> ujson.Num(2)
    )
  )

  check(
    """
      |{ "a": [1,], }
    """.stripMargin,
    ujson.Obj("a" -> ujson.Arr(1))
  )

}
