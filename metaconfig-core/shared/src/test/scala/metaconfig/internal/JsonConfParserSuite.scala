package metaconfig.internal

import metaconfig.Conf
import metaconfig.Input
import org.scalatest.FunSuite
import ujson._

class JsonConfParserSuite extends FunSuite {
  class ConfVisitor(input: Input) extends AstTransformer[Conf] {
    override def transform[T](j: Conf, f: Visitor[_, T]): T = ???

    override def visitArray(index: Int): ArrVisitor[Conf, Conf] = ???

    override def visitObject(index: Int): ObjVisitor[Conf, Conf] = ???

    override def visitNull(index: Int): Conf = ???

    override def visitFalse(index: Int): Conf = ???

    override def visitTrue(index: Int): Conf = ???

    override def visitNum(
        s: CharSequence,
        decIndex: Int,
        expIndex: Int,
        index: Int): Conf = ???

    override def visitString(s: CharSequence, index: Int): Conf = ???
  }
  def skip(original: String, expected: Js): Unit = {
    ignore(original) {}
  }
  def check(original: String, expected: Js): Unit = {
    test(original) {
      val transformable =
        Transformable.fromTransformer(original, JsonConfParser)
      val js = transformable.transform(Js)
      assert(js == expected)
    }
  }

  skip(
    """{
      |  "a":1
      |}""".stripMargin,
    Js.Obj("a" -> Js.Num(1))
  )

  // comments
  skip(
    """{
      |  // leading
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
      |    1,
      |
      |  ],
      |  "a": 2,
      |
      |}
    """.stripMargin,
    Js.Obj(
      "b" -> Js.Arr(Js.Num(1)),
      "a" -> Js.Num(2)
    )
  )

}
