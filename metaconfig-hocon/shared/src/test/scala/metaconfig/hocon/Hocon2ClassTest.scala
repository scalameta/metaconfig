package metaconfig
package hocon

import metaconfig.DeriveConfDecoder
import org.scalatest.FunSuite

@DeriveConfDecoder
case class MyConfig(
    a: Int = 22,
    b: String = "banana"
)

class Hocon2ClassTest extends FunSuite {

  def check(config: String, expected: Conf): Unit = {
    test(expected.show) {
      val Right(obtained) = Hocon2Class.gimmeConfig(config)
      assert(obtained.normalize == expected)
    }
  }
  import Conf._

  check(
    """
      |a = 2
      |a.b = 3
    """.stripMargin,
    Obj("a" -> Obj("b" -> Num(3)))
  )
  check(
    """
      |a = [1]
      |a = [2]
    """.stripMargin,
    Obj("a" -> Lst(Num(2)))
  )

  test("field 'a' is overwritten") {
    val default = MyConfig()
    val config: String =
      """
        |a = 666
      """.stripMargin
    val Right(obtained) =
      Hocon2Class.gimmeClass[MyConfig](config, default.reader)
    val expected = default.copy(a = 666)
    assert(obtained == expected)
  }

}
