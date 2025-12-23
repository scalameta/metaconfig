package metaconfig

import metaconfig.generic.{Settings, Surface}
import metaconfig.pprint.TPrintColors

class DeriveSurfaceSuite extends munit.FunSuite {

  case class Curried(a: Int)(b: List[Int])
  test("curried") {
    val surface = generic.deriveSurface[Curried]
    val List(a :: Nil, b :: Nil) = surface.fields: @unchecked
    assertNoDiff(a.name, "a")
    assertNoDiff(b.name, "b")
  }

  case class Underlying(number: Int)
  object Underlying {
    implicit val surface: Surface[Underlying] = generic.deriveSurface[Underlying]
  }
  case class Enclosing(underlying: Underlying)
  object Enclosing {
    implicit val surface: Surface[Enclosing] = generic.deriveSurface[Enclosing]
  }
  test("underlying") {
    val surface = generic.deriveSurface[Enclosing]
    val List(underlying :: Nil) = surface.fields: @unchecked
    val List(number :: Nil) = underlying.underlying: @unchecked
    assertNoDiff(underlying.name, "underlying")
    assertNoDiff(number.name, "number")
  }

  case class TypeParam[T](value: T)
  object TypeParam {
    implicit def surface[T]: Surface[TypeParam[T]] = generic
      .deriveSurface[TypeParam[T]]
  }
  test("tparam") {
    implicit val is: Surface[Int] = new Surface[Int](Nil)
    val surface = TypeParam.surface[Int]
    val List(value :: Nil) = surface.fields: @unchecked
    assertNoDiff(value.name, "value")
    assertNoDiff(value.tpe, "T")
  }

  case class AllRepeated[T](
      notIterable: String,
      a: Iterable[T],
      b: List[Int],
      c: Set[String],
  )
  object AllRepeated {
    implicit def surface[T]: Surface[AllRepeated[T]] = generic
      .deriveSurface[AllRepeated[T]]
  }

  test("@Repeated") {
    val settings = Settings[AllRepeated[Int]]
    assert(settings.settings.length == 4)
    val notIterable :: tail = settings.settings: @unchecked
    assert(!notIterable.isRepeated)
    tail.foreach(setting => assert(setting.isRepeated, setting.name))
  }

  case class CustomTypePrinting(a: Int, b: Option[Int], c: List[String])
  test("tprint") {
    import metaconfig.pprint.TPrint
    implicit val intPrint = new TPrint[Int] {
      def render(implicit tpc: TPrintColors): fansi.Str = fansi.Str("number")
    }
    implicit def optionPrint[T](implicit ev: TPrint[T]): TPrint[Option[T]] =
      new TPrint[Option[T]] {
        def render(implicit tpc: TPrintColors): fansi.Str = "(" + ev.render +
          ")"
      }
    implicit def iterablePrint[C[x] <: Iterable[x], T](implicit
        ev: TPrint[T],
    ): TPrint[C[T]] = new TPrint[C[T]] {
      def render(implicit tpc: TPrintColors): fansi.Str = "[" + ev.render +
        " ...]"
    }
    implicit val surface = generic.deriveSurface[CustomTypePrinting]
    val a :: b :: c :: Nil = Settings[CustomTypePrinting].settings: @unchecked
    assertEquals(a.tpe, "number")
    assertEquals(b.tpe, "(number)")
    assertEquals(c.tpe, "[String ...]")
  }
}
