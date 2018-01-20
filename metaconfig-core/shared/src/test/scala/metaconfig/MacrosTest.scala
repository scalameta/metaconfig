package metaconfig

import metaconfig.Conf._
import metaconfig.internal.Macros
import org.scalatest.FunSuite

case class AllTheAnnotations(
    @SettingDescription("descriptioon")
    @ExampleValue("value")
    @ExampleValue("value2")
    @ExtraSettingName("extraName")
    @ExtraSettingName("extraName2")
    @DeprecatedSettingName("deprecatedName", "Use x instead", "2.0")
    @DeprecatedSettingName("deprecatedName2", "Use y instead", "3.0")
    @SinceVersion("2.1")
    @SettingDescription("Description")
    @DeprecatedSetting("Use newFeature instead", "2.1")
    number: Int = 2,
    string: String = "string",
    lst: List[String] = Nil
)

object AllTheAnnotations {
  implicit lazy val fields: Surface[AllTheAnnotations] =
    Macros.deriveSurface[AllTheAnnotations]
  lazy val settings = Settings[AllTheAnnotations]
  implicit lazy val decoder: ConfDecoder[AllTheAnnotations] =
    Macros.deriveConfDecoder[AllTheAnnotations](AllTheAnnotations())
}

class MacrosTest extends FunSuite {

  def checkError(name: String, obj: Conf, expected: String): Unit = {
    test(name) {
      ConfDecoder.decode[AllTheAnnotations](obj) match {
        case Configured.NotOk(err) =>
          assert(expected == err.toString)
        case Configured.Ok(obtained) =>
          fail(s"Expected error, obtained=$obtained")
      }
    }
  }

  private val number = "number" -> Num(42)
  private val string = "string" -> Str("42")

//  checkError(
//    "typo",
//    Obj(number, "sttring" -> Str("42")),
//    ""
//  )

  test("ConfDecoder[T] ok") {
    val obj = Obj(
      "number" -> Num(42),
      "string" -> Str("42"),
      "lst" -> Lst(Str("43") :: Nil)
    )
    val expected = AllTheAnnotations(42, "42", List("43"))
    val obtained = ConfDecoder.decode[AllTheAnnotations](obj).get
    pprint.log(obtained)
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
          DeprecatedSettingName("deprecatedName", "Use x instead", "2.0"),
          DeprecatedSettingName("deprecatedName2", "Use y instead", "3.0"))
    )
    assert(
      s1.exampleValues ==
        List("value", "value2")
    )
    assert(s1.description.contains("descriptioon"))
    assert(s1.sinceVersion.contains("2.1"))
    assert(
      s1.deprecated.contains(DeprecatedSetting("Use newFeature instead", "2.1"))
    )

    assert(s2.name == "string")
    assert(s2.annotations.isEmpty)
  }
}
