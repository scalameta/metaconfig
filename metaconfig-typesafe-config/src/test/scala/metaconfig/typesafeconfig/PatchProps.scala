package metaconfig.typesafeconfig

import metaconfig.Conf
import metaconfig.ConfOps
import metaconfig.ConfShow
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalameta.logger
import org.scalatest.FunSuite
import scala.meta.testkit.DiffAssertions
import metaconfig.Generators.argConfShow

object PatchProps {
  // asserts that applying
  def checkPatch(a: String, b: String): Boolean = {
    val original = Conf.parseString(a).get
    val revised = Conf.parseString(b).get
    val patch = Conf.patch(original, revised)
    val expected = ConfOps.merge(original, revised)
    val obtained = ConfOps.merge(original, patch)
    if (obtained != expected) {
      logger.elem(
        obtained,
        expected,
        patch.toString,
        Conf.patch(obtained, expected)
      )
    }
    obtained == expected
  }
}

class PatchProps extends Properties("Patch") {

  property("roundtrip") = forAll { (a: ConfShow, b: ConfShow) =>
    PatchProps.checkPatch(a.str, b.str)
  }

}
class PatchPropsSuite extends FunSuite with DiffAssertions {
  def check(a: String, b: String): Unit = {
    test(a) { assert(PatchProps.checkPatch(a, b)) }
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
    """.stripMargin
  )

}
