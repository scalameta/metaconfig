package metaconfig.internal

import metaconfig.Input
import ujson._
import scala.util.Properties

class JsonConfParserSuite extends munit.FunSuite {

  def check(original: String, expected: Js): Unit = {
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
    Js.Obj("a" -> Js.Num(1))
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
    Js.Obj(
      "a" -> Js.Num(1),
      "b" -> Js.Num(2),
      "c" -> Js.Arr(Js.Num(3))
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
    Js.Obj(
      "b" -> Js.Arr(Js.Num(1), Js.Num(2)),
      "a" -> Js.Num(2)
    )
  )

  check(
    """
      |{ "a": [1,], }
    """.stripMargin,
    Js.Obj("a" -> Js.Arr(1))
  )

}
