package metaconfig

import java.io.File
import org.scalatest.FunSuite

class DeriveSurfaceSuite extends FunSuite {

  case class WithFile(file: File)
  test("toString") {
    val surface = generic.deriveSurface[WithFile]
    val obtained = surface.toString
    val expected =
      """Surface(List(List(Field(name="file",tpe="java.io.File",annotations=List(),underlying=List()))))"""
    assert(obtained == expected)
  }

  case class Curried(a: Int)(b: List[Int])
  test("curried") {
    val surface = generic.deriveSurface[Curried]
    val List(a :: Nil, b :: Nil) = surface.fields
    assert(a.name == "a")
    assert(b.name == "b")
  }

  case class Underlying(number: Int)
  object Underlying { implicit val surface = generic.deriveSurface[Underlying] }
  case class Enclosing(underlying: Underlying)
  object Enclosing { implicit val surface = generic.deriveSurface[Enclosing] }
  test("underlying") {
    val surface = generic.deriveSurface[Enclosing]
    val List(underlying :: Nil) = surface.fields
    val List(number :: Nil) = underlying.underlying
    assert(underlying.name == "underlying")
    assert(number.name == "number")
  }

  object MyObject
  test("object") {
    assertDoesNotCompile("generic.deriveSurface[MyObject.type]")
  }

  trait MyTrait
  test("triat") {
    assertDoesNotCompile("generic.deriveSurface[MyTrait.type]")
  }

  class MissingCase(a: Int)
  test("case") {
    assertDoesNotCompile("generic.deriveSurface[MissingCase]")
  }
}
