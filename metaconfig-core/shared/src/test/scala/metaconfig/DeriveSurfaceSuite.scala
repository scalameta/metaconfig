package metaconfig

import java.io.File
import metaconfig.generic.Surface
import org.scalatest.FunSuite

class DeriveSurfaceSuite extends FunSuite {

  def check[T](
      name: String,
      surface: => Surface[T],
      expected: => String): Unit = {
    test(name) {
      assert(surface.toString == expected)
    }
  }

  case class WithFile(file: File)
  check(
    "simple",
    generic.deriveSurface[WithFile],
    """Surface(List(List(Field(name="file",tpe="java.io.File",annotations=List(),underlying=List()))))"""
  )

  case class Curried(a: Int)(b: List[Int])
  check(
    "curried",
    generic.deriveSurface[Curried],
    """Surface(List(List(Field(name="a",tpe="Int",annotations=List(),underlying=List())), List(Field(name="b",tpe="List[Int]",annotations=List(),underlying=List()))))"""
  )

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
