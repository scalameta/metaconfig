package metaconfig

import org.scalatest.FunSuite

class SettingsSuite extends FunSuite {

  case class ToString(@ExtraName("extra") name: String)
  test("Settings[T].toString") {
    implicit val surface = generic.deriveSurface[ToString]
    val obtained = Settings[ToString].toString
    val expected = """Surface(settings=List(Setting(Field(name="name",tpe="String",annotations=List(@ExtraName(extra))))))"""
    assert(obtained == expected)
  }

  test("Settings[T]") {
    val List(s1, s2, _) = Settings[AllTheAnnotations].settings
    assert(s1.name == "number")
    assert(
      s1.extraNames == List(
        "extraName",
        "extraName2"
      )
    )
    assert(
      s1.deprecatedNames ==
        List(
          DeprecatedName("deprecatedName", "Use x instead", "2.0"),
          DeprecatedName("deprecatedName2", "Use y instead", "3.0"))
    )
    assert(
      s1.exampleValues ==
        List("value", "value2")
    )
    assert(s1.description.contains("descriptioon"))
    assert(s1.sinceVersion.contains("2.1"))
    assert(
      s1.deprecated.contains(Deprecated("Use newFeature instead", "2.1"))
    )

    assert(s2.name == "string")
    assert(s2.annotations.isEmpty)
  }
}
