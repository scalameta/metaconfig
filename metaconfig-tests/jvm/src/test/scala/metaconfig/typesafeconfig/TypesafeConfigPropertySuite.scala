package metaconfig.typesafeconfig

import metaconfig.Conf
import metaconfig.ConfShow
import metaconfig.Generators.argConfShow

import org.scalacheck.Prop.forAll

class TypesafeConfigPropertySuite extends munit.ScalaCheckSuite {
  def check(a: String, b: String): Unit = test(a)(assertRoundtrip(a, b))

  def assertRoundtrip(a: String, b: String): Unit = {
    val original = Conf.parseString(a).get
    val revised = Conf.parseString(b).get
    val patch = Conf.patch(original, revised)
    val expected = Conf.applyPatch(original, revised)
    val obtained = Conf.applyPatch(original, patch)
    assertEquals(obtained, expected)
  }

  property("roundtrip") {
    forAll((a: ConfShow, b: ConfShow) => assertRoundtrip(a.str, b.str))
  }

  check(
    """
      |ad.da = true
      |cc.bd = "dd"
    """.stripMargin,
    """
      |
      |ad.a.dc = false
      |ad = "ad"
    """.stripMargin,
  )

}
