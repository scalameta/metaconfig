package metaconfig

import metaconfig.internal.CanBuildFromDecoder

object ConfListTest {

  case class Bar(add: List[String] = Nil)

  implicit val barSurface: generic.Surface[Bar] =
    generic.deriveSurface

  implicit val barReader: ConfDecoder[Bar] =
    generic.deriveDecoder[Bar](Bar()).noTypos

  case class Foo(
      field: List[String] = List("c"),
      anotherField: List[String] = Nil,
      bar: Bar = Bar()
  )
  implicit val surface: generic.Surface[Foo] =
    generic.deriveSurface

  implicit val reader: ConfDecoder[Foo] =
    generic.deriveDecoder[Foo](Foo()).noTypos
}

class ConfListTest extends munit.FunSuite {
  import ConfListTest.{Foo, Bar}
  import Conf._

  test("simple") {
    val conf = Obj("field" -> Lst(Str("a"), Str("b")))
    val obtained = conf.as[Foo].get
    val expected = Foo(List("a", "b"))
    assertEquals(obtained, expected)
  }

  test("missing") {
    val conf = Obj()
    val obtained = conf.as[Foo].get
    val expected = Foo(List("c"), Nil)
    assertEquals(obtained, expected)
  }

  test("add to default") {
    val conf = Obj("field" -> Obj("add" -> Lst(Str("a"), Str("b"))))
    val obtained = conf.as[Foo].get
    val expected = Foo(List("c", "a", "b"))
    assertEquals(obtained, expected)
  }

  test("don't touch another fields with same type") {
    val conf = Obj(
      "field" -> Obj("add" -> Lst(Str("a"), Str("b"))),
      "anotherField" -> Lst(Str("d"))
    )
    val obtained = conf.as[Foo].get
    val expected = Foo(List("c", "a", "b"), List("d"))
    assertEquals(obtained, expected)
  }

  test("read config from foo.bar.add") {
    val conf = Obj(
      "field" -> Obj("add" -> Lst(Str("a"), Str("b"))),
      "anotherField" -> Lst(Str("d")),
      "bar" -> Obj("add" -> Lst(Str("e")))
    )
    val obtained = conf.as[Foo].get
    val expected = Foo(List("c", "a", "b"), List("d"), Bar(List("e")))
    assertEquals(obtained, expected)
  }

}
