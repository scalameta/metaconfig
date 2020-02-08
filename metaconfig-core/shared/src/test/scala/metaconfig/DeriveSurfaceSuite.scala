package metaconfig

import java.io.File
import metaconfig.generic.Settings
import metaconfig.generic.Surface

class DeriveSurfaceSuite extends munit.FunSuite {

  case class WithFile(file: File)
  test("toString") {
    val surface = generic.deriveSurface[WithFile]
    val obtained = surface.toString
    val expected =
      """Surface(List(List(Field(name="file",tpe="File",annotations=List(),underlying=List()))))"""
    assertEquals(obtained, expected)
  }

  case class Curried(a: Int)(b: List[Int])
  test("curried") {
    val surface = generic.deriveSurface[Curried]
    val List(a :: Nil, b :: Nil) = surface.fields
    assertNoDiff(a.name, "a")
    assertNoDiff(b.name, "b")
  }

  case class Underlying(number: Int)
  object Underlying { implicit val surface = generic.deriveSurface[Underlying] }
  case class Enclosing(underlying: Underlying)
  object Enclosing { implicit val surface = generic.deriveSurface[Enclosing] }
  test("underlying") {
    val surface = generic.deriveSurface[Enclosing]
    val List(underlying :: Nil) = surface.fields
    val List(number :: Nil) = underlying.underlying
    assertNoDiff(underlying.name, "underlying")
    assertNoDiff(number.name, "number")
  }

  case class TypeParam[T](value: T)
  object TypeParam {
    implicit def surface[T]: Surface[TypeParam[T]] =
      generic.deriveSurface[TypeParam[T]]
  }
  test("tparam") {
    implicit val is: Surface[Int] = new Surface[Int](Nil)
    val surface = TypeParam.surface[Int]
    val List(value :: Nil) = surface.fields
    assertNoDiff(value.name, "value")
    assertNoDiff(value.tpe, "T")
  }

  case class AllRepeated[T](
      notIterable: String,
      a: Iterable[T],
      b: List[Int],
      c: Set[String]
  )
  object AllRepeated {
    implicit def surface[T]: Surface[AllRepeated[T]] =
      generic.deriveSurface[AllRepeated[T]]
  }

  test("@Repeated") {
    val settings = Settings[AllRepeated[Int]]
    assert(settings.settings.length == 4)
    val notIterable :: tail = settings.settings
    assert(!notIterable.isRepeated)
    tail.foreach { setting =>
      assert(setting.isRepeated, setting.name)
    }
  }

  case class CustomTypePrinting(a: Int, b: Option[Int], c: List[String])
  test("tprint") {
    import pprint.TPrint
    implicit val intPrint = TPrint.literal[Int]("number")
    implicit def optionPrint[T](implicit ev: TPrint[T]): TPrint[Option[T]] =
      TPrint.make { implicit colors =>
        "(" + ev.render + ")"
      }
    implicit def iterablePrint[C[x] <: Iterable[x], T](
        implicit ev: TPrint[T]
    ): TPrint[C[T]] =
      TPrint.make { implicit colors =>
        "[" + ev.render + " ...]"
      }
    implicit val surface = generic.deriveSurface[CustomTypePrinting]
    val a :: b :: c :: Nil = Settings[CustomTypePrinting].settings
    assert(a.tpe == "number")
    assert(b.tpe == "(number)")
    assert(c.tpe == "[String ...]")
  }
}
