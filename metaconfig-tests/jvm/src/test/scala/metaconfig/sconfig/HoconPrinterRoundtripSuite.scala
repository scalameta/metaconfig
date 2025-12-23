package metaconfig.sconfig

import metaconfig.Generators.argConfShow
import metaconfig.{Conf, ConfShow}

import org.scalacheck.Prop.forAll

class HoconPrinterRoundtripSuite extends munit.ScalaCheckSuite {
  def assertRoundtrip(conf: String): Unit = {
    val a = Conf.parseString(conf).get
    val hocon = Conf.printHocon(a)
    val b = Conf.parseString(hocon).get
    assertEquals(a, b)
  }
  def ignore(conf: String): Unit = super.test(conf.ignore) {}
  def checkRoundtrip(conf: String): Unit =
    test(conf.take(100))(assertRoundtrip(clue(conf)))

  ignore(
    """
      |a.a = "d"
      |a.bc = 9
    """.stripMargin,
  )

  checkRoundtrip(
    """
      |aa.bb = true
      |aa.d = 3
      |aa.aa = "cb"
    """.stripMargin,
  )

  property("roundtrip")(forAll((conf: ConfShow) => assertRoundtrip(conf.str)))
}
