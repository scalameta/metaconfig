package metaconfig
package hocon

import scala.meta.inputs.Input

import org.scalatest.FunSuite

case class MyConfig(
    a: Int = 22,
    b: String = "banana"
) {
  val reader: ConfDecoder[MyConfig] = ConfDecoder.instanceF[MyConfig] { c =>
    (
      c.getOrElse[Int]("a")(a) |@|
        c.getOrElse[String]("b")(b)
    ).map(MyConfig.tupled.apply)
  }
}

class Hocon2ClassTest extends FunSuite {

  def check(config: String, expected: Conf): Unit = {
    test(expected.show) {
      val Configured.Ok(obtained) =
        Hocon2Class.gimmeConfig(Input.String(config))
      assert(obtained == expected)
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
    val Configured.Ok(obtained) =
      Hocon2Class.gimmeClass[MyConfig](Input.String(config), default.reader)
    val expected = default.copy(a = 666)
    assert(obtained == expected)
  }

}
