package metaconfig.hocon

import metaconfig.ConfigReader
import org.scalatest.FunSuite

class Hocon2ClassTest extends FunSuite {
  @ConfigReader
  case class MyConfig(
      a: Int = 22,
      b: String = "banana"
  )
  val default = MyConfig()
  val config =
    """
      |a = 666
    """.stripMargin

  test("field 'a' is overwritten") {
    val Right(obtained) =
      Hocon2Class.gimmeClass[MyConfig](config, default.reader)
    val expected = default.copy(a = 666)
    assert(obtained == expected)
  }

}
