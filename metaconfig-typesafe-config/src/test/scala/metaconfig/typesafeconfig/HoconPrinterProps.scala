package metaconfig.typesafeconfig

import metaconfig.Conf
import metaconfig.ConfOps
import metaconfig.ConfShow
import org.scalacheck.Properties
import org.scalameta.logger
import scala.meta.testkit.DiffAssertions
import metaconfig.Generators.argConfShow
import org.scalacheck.Prop.forAll

object HoconPrinterProps {
  def checkRoundtrip(conf: String): Boolean = {
    val a = Conf.parseString(conf).get
    val hocon = Conf.printHocon(a)
    val b = Conf.parseString(hocon).get
    val isEqual = a == b
    if (!isEqual) {
      pprint.log(a)
      pprint.log(b)
      logger.elem(conf, hocon, Conf.patch(a, b))
    }
    a == b
  }

}

class HoconPrinterProps extends Properties("HoconPrinter") {
  property("roundtrip") = forAll { conf: ConfShow =>
    HoconPrinterProps.checkRoundtrip(conf.str)
  }
}

class HoconPrinterRoundtripSuite extends munit.FunSuite {
  def ignore(conf: String): Unit = super.test(conf.ignore) {}
  def checkRoundtrip(conf: String): Unit =
    test(conf.take(100)) {
      assert(HoconPrinterProps.checkRoundtrip(conf))
    }

  ignore(
    """
      |a.a = "d"
      |a.bc = 9
    """.stripMargin
  )

  checkRoundtrip(
    """
      |aa.bb = true
      |aa.d = 3
      |aa.aa = "cb"
    """.stripMargin
  )
}
