package metaconfig.internal

import scala.meta.testkit.DiffAssertions
import org.scalatest.FunSuite

class CaseSuite extends FunSuite with DiffAssertions {
  def checkKebabToCamel(original: String, expected: String): Unit = {
    check(original, expected, Case.kebabToCamel)
  }
  def checkCamelToKebab(original: String, expected: String): Unit = {
    check(original, expected, Case.camelToKebab)
  }
  def check(original: String, expected: String, f: String => String): Unit = {
    test(original) {
      val obtained = f(original)
      assertNoDiff(obtained, expected)
    }
  }

  checkKebabToCamel("a-b", "aB")
  checkKebabToCamel("a-b-c", "aBC")
  checkKebabToCamel("a-bo-co", "aBoCo")
  checkKebabToCamel("-a-bo-co", "ABoCo")
  checkKebabToCamel("-a-bo-co-", "ABoCo-")

  checkCamelToKebab("fooBar", "foo-bar")
  checkCamelToKebab("A", "-a")
  checkCamelToKebab("AB", "-a-b")

}
