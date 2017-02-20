package metaconfig.hocon

import metaconfig.ConfigReader
import org.scalatest.FunSuite

@ConfigReader
case class MyConfig(
    a: Int = 22,
    b: String = "banana"
)

class Hocon2ClassTest extends FunSuite {
  val default = MyConfig()
  val config =
    """
      |a = 666
    """.stripMargin

  test("basic") {
    val Right(obtained) =
      Hocon2Class.gimmeClass[MyConfig](config, default.reader)
    val expected = default.copy(a = 666)
    assert(obtained == expected)
  }

}
