package mopt

case class Foo(a: Option[String])
object Foo {
  implicit val reader: ConfDecoder[Foo] =
    ConfDecoder.instance[Foo] {
      case obj: Conf.Obj =>
        obj.getOption[String]("a").map(Foo(_))
    }
}

class ConfOptionSuite extends munit.FunSuite {
  import Conf._

  test("simple") {
    val conf = Obj("a" -> Str("b"))
    val obtained = conf.as[Foo].get
    val expected = Foo(Some("b"))
    assertEquals(obtained, expected)
  }

  test("missing") {
    val conf = Obj()
    val obtained = conf.as[Foo].get
    val expected = Foo(None)
    assertEquals(obtained, expected)
  }
}
