package metaconfig.sconfig

import metaconfig.Conf
import metaconfig.Configured
import metaconfig.Input
import metaconfig.PlatformInput

import java.nio.file.Paths

import munit.FunSuite

class InputSuite extends FunSuite {

  private val confPath = Paths
    .get("metaconfig-tests/shared/src/test/resources/metaconfig/input_test")
  private val confStr =
    """|version = "1.2.3"
       |
       |newlines {
       |  source = fold
       |  configStyle {
       |    callSite.preset = none
       |    defnSite.force = true
       |  }
       |}
       |""".stripMargin
  private val confObj = Conf.Obj(
    "newlines" -> Conf.Obj(
      "source" -> Conf.Str("fold"),
      "configStyle" -> Conf.Obj(
        "callSite" -> Conf.Obj("preset" -> Conf.Str("none")),
        "defnSite" -> Conf.Obj("force" -> Conf.Bool(true)),
      ),
    ),
    "version" -> Conf.Str("1.2.3"),
  )

  test("PlatformInput.readFile") {
    assertEquals(PlatformInput.readFile(confPath, "utf-8"), confStr)
  }

  test("SConfig2Class.gimmeConfFromString") {
    assertEquals(
      SConfig2Class.gimmeConfFromString(confStr),
      Configured.Ok(confObj),
    )
  }

  test("sConfigMetaconfigParser.fromInput") {
    assertEquals(
      sConfigMetaconfigParser.fromInput(Input.File(confPath)),
      Configured.Ok(confObj),
    )
  }

}
