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

  case class Mapped(raw: String)
  case class Baz(as: List[Mapped] = List(Mapped("a")))

  implicit val readerListA: ConfDecoderWithDefault[List[Mapped]] =
    implicitly[ConfDecoderWithDefault[List[String]]]
      .mapWithDefault(x => x.map(a => Mapped(a + "_mapped")))(_.map(_.raw))

  implicit val surfaceBaz: generic.Surface[Baz] =
    generic.deriveSurface

  implicit val readerBaz: ConfDecoder[Baz] =
    generic.deriveDecoder[Baz](Baz()).noTypos
}

class ConfListTest extends munit.FunSuite {
  import ConfListTest.{Foo, Bar, Baz, Mapped}
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

  test("map appendable values") {
    val conf = Obj("as" -> Obj("add" -> Lst(Str("b"))))
    val obtained = conf.as[Baz].get
    val expected = Baz(List(Mapped("a_mapped"), Mapped("b_mapped")))
    assertEquals(obtained, expected)
  }

}
