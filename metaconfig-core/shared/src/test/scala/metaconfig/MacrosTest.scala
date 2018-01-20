package metaconfig

import metaconfig.internal.Macros
import org.scalatest.FunSuite

case class AllTheAnnotations(
    @SettingDescription("descriptioon")
    @ExampleValue("value")
    @ExampleValue("value2")
    @ExtraSettingName("extraName")
    @ExtraSettingName("extraName2")
    @DeprecatedSettingName("deprecatedName")
    @DeprecatedSettingName("deprecatedName2")
    @SinceVersion("2.1")
    @SettingDescription("Description")
    @DeprecatedSetting("Use newFeature instead", "2.1")
    setting: Int,
    setting2: String
)

object AllTheAnnotations {
  implicit val settings: Settings[AllTheAnnotations] =
    Macros.deriveSettings[AllTheAnnotations]
}

class MacrosTest extends FunSuite {
  test("macros rock") {
    val List(s1, s2) = Settings[AllTheAnnotations].settings
    assert(s1.name == SettingName("setting"))
    assert(
      s1.extraNames == List(
        ExtraSettingName("extraName2"),
        ExtraSettingName("extraName"))
    )
    assert(
      s1.deprecatedNames ==
        List(
          DeprecatedSettingName("deprecatedName2"),
          DeprecatedSettingName("deprecatedName"))
    )
    assert(
      s1.exampleValues ==
        List(ExampleValue("value2"), ExampleValue("value"))
    )
    assert(s1.description.contains(SettingDescription("descriptioon")))
    assert(s1.sinceVersion.contains(SinceVersion("2.1")))
    assert(
      s1.deprecated.contains(DeprecatedSetting("Use newFeature instead", "2.1"))
    )

    assert(s2.name == SettingName("setting2"))
    assert(s2.extraNames.isEmpty)
    assert(s2.deprecatedNames.isEmpty)
    assert(s2.exampleValues.isEmpty)
    assert(s2.description.isEmpty)
    assert(s2.sinceVersion.isEmpty)
    assert(s2.deprecated.isEmpty)
  }
}
