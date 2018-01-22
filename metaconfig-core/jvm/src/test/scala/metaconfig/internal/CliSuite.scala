package metaconfig.internal

import scala.meta.testkit.DiffAssertions
import metaconfig.generic.Settings
import org.scalatest.FunSuite

class CliSuite extends FunSuite with DiffAssertions {
  test("help") {
    val obtained = Settings[Options].toCliHelp(default = Options(cwd = "/tmp"))
    val expected =
      """
        |--in: String = docs                                 The input directory to
        |                                                    generate the fox site.
        |--out: String = target/fox                          The output directory to
        |                                                    generate the fox site.
        |--cwd: String = /tmp
        |--repo-name: String = olafurpg/fox
        |--repo-url: String = https://github.com/olafurpg/fox
        |--title: String = Fox
        |--description: String = My Description
        |--google-analytics: List[String] = List()
        |--classpath: List[String] = List()
        |--clean-target: Boolean = false
        |--base-url: String =
        |--encoding: String = UTF-8
        |--config-path: String = fox.conf
        |--remaining-args: List[String] = List()
      """.stripMargin
    assertNoDiff(obtained, expected)
  }

}
