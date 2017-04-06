package metaconfig

import org.scalatest.FunSuite

class DerivationTest extends FunSuite {
  type Result[T] = Either[Throwable, T]

  @DeriveConfDecoder
  case class Inner(nest: Int)

  @DeriveConfDecoder
  case class Outer(i: Int, inner: Inner) {
    implicit val innerReader: ConfDecoder[Inner] = inner.reader
  }

  @DeriveConfDecoder
  case class OuterRecurse(i: Int, @metaconfig.Recurse inner: Inner)

  @DeriveConfDecoder
  case class Bar(i: Int, b: Boolean, s: String)

  @DeriveConfDecoder
  case class HasList(i: Seq[Int])

  @DeriveConfDecoder
  case class HasMap(i: Map[String, Int])

  val b = Bar(0, true, "str")
  test("invalid field") {
    assert(
      b.reader.read(Conf.Obj("is" -> Conf.Num(2), "var" -> Conf.Num(3))) ==
        Left(InvalidField("Bar", List("is", "var"))))
  }

  test("read OK") {
    assert(
      b.reader.read(Conf.Obj("i" -> Conf.Num(2))) ==
        Right(b.copy(i = 2)))
    assert(
      b.reader.read(Conf.Obj("s" -> Conf.Str("str"))) ==
        Right(b.copy(s = "str")))
    assert(
      b.reader.read(Conf.Obj("b" -> Conf.Bool(true))) ==
        Right(b.copy(b = true)))
    assert(
      b.reader.read(
        Conf.Obj(
          "i" -> Conf.Num(3),
          "b" -> Conf.Bool(true),
          "s" -> Conf.Str("rand")
        )) == Right(b.copy(i = 3, s = "rand", b = true)))
  }
  test("unexpected type") {
    val msg =
      "Error reading field 'i'. Expected argument of type int. Obtained value '\"str\"' of type String."
    val Left(e: IllegalArgumentException) =
      b.reader.read(Conf.Obj("i" -> Conf.Str("str")))
    assert(e.getCause != null)
  }

  test("write OK") {
    assert(
      b.fields == Map(
        "i" -> 0,
        "b" -> true,
        "s" -> "str"
      ))
  }
  test("nested OK") {
    val m = Conf.Obj(
      "i" -> Conf.Num(4),
      "inner" -> Conf.Obj(
        "nest" -> Conf.Num(5)
      )
    )
    val Right(n) = OuterRecurse(2, Inner(3)).reader.read(m)
    val Right(o) = Outer(2, Inner(3)).reader.read(m)
    assert(o == Outer(4, Inner(5)))
    assert(n == OuterRecurse(4, Inner(5)))
  }

  test("Seq") {
    val lst = HasList(List(1, 2, 3))
    assert(
      lst.reader
        .read(Conf.Obj("i" -> Conf.Lst(Conf.Num(666), Conf.Num(777)))) ==
        Right(HasList(Seq(666, 777))))
  }

  test("Conf.Obj") {
    val lst = HasMap(Map("1" -> 2))
    assert(
      lst.reader.read(Conf.Obj("i" -> Conf.Obj("666" -> Conf.Num(777)))) ==
        Right(HasMap(Map("666" -> 777))))
  }

  case object Kase
  @DeriveConfDecoder
  case class Ob(kase: Kase.type) {
    implicit val KaseReader: ConfDecoder[Kase.type] =
      ConfDecoder.stringR.flatMap { x =>
        ???
      }
  }

  test("Runtime ???") {
    val m = Conf.Obj(
      "kase" -> Conf.Str("string")
    )
    val Left(e: IllegalArgumentException) =
      Ob(Kase).reader.read(m)
    assert(e.getCause != null)
  }

  @DeriveConfDecoder
  case class HasExtra(@ExtraName("b") @metaconfig.ExtraName("c") a: Int)
  test("@ExtraName") {
    val x = HasExtra(1)
    val Right(HasExtra(2)) = x.reader.read(Conf.Obj("b" -> Conf.Num(2)))
    val Right(HasExtra(3)) = x.reader.read(Conf.Obj("c" -> Conf.Num(3)))
    val Left(_) = x.reader.read(Conf.Obj("d" -> Conf.Num(3)))
  }
}
