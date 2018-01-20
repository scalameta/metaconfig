package metaconfig

import metaconfig.internal.Macros
import org.scalatest.FunSuite

class ObjectFactorySuite extends FunSuite {
  case class Foo(a: Int, b: String)(c: List[String])
  object Foo {
    implicit val surface: Surface[Foo] = Macros.deriveSurface[Foo]
  }
  def checkOk(expected: Foo, argss: List[List[Any]]): Unit = {
    test(expected.toString) {
      val Right(obtained) = Surface[Foo].factory.newInstance(argss)
      assert(obtained == expected)
    }
  }

  checkOk(
    Foo(1, "1")(Nil),
    List(List(1, "1"), List(Nil))
  )

  checkOk(
    Foo(1, "2")(List("3")),
    List(List(1, "2"), List(List("3")))
  )
}
