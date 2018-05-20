package metaconfig.internal

import metaconfig.Input
import org.scalatest.FunSuite
import ujson._

class JsonConfParserSuite extends FunSuite {

  def skip(original: String, expected: Js): Unit =
    ignore(original) {}

  def check(original: String, expected: Js): Unit = {
    test(original) {
      val transformable =
        Transformable.fromTransformer(Input.String(original), JsonConfParser)
      val js = transformable.transform(Js)
      assert(js == expected)
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
