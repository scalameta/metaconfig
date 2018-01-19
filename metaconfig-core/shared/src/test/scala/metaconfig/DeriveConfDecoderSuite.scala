package metaconfig

import java.io.File
import metaconfig.Conf._
import metaconfig.internal.Macros
import org.scalatest.FunSuite

case class AllTheAnnotations(
    @Description("descriptioon")
    @ExampleValue("value")
    @ExampleValue("value2")
    @ExtraName("extraName")
    @ExtraName("extraName2")
    @DeprecatedName("deprecatedName", "Use x instead", "2.0")
    @DeprecatedName("deprecatedName2", "Use y instead", "3.0")
    @SinceVersion("2.1")
    @Description("Description")
    @Deprecated("Use newFeature instead", "2.1")
    number: Int = 2,
    string: String = "string",
    lst: List[String] = Nil
)

object AllTheAnnotations {
  implicit lazy val fields: Surface[AllTheAnnotations] =
    Macros.deriveSurface[AllTheAnnotations]
  lazy val settings = Settings[AllTheAnnotations]
  implicit lazy val decoder: ConfDecoder[AllTheAnnotations] =
    Macros.deriveConfDecoder[AllTheAnnotations](AllTheAnnotations()).noTypos
}

class DeriveSurfaceSuite extends FunSuite {

  case class WithFile(file: File)
  test("Surface[T]") {
    assertCompiles("Macros.deriveSurface[WithFile]")
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
class DeriveConfDecoderSuite extends FunSuite {

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
  def checkOk(name: String, obj: Conf, expected: AllTheAnnotations): Unit = {
    test(name) {
      ConfDecoder.decode[AllTheAnnotations](obj) match {
        case Configured.NotOk(err) =>
          fail(err.toString)
        case Configured.Ok(obtained) =>
          assert(obtained == expected)
      }
    }
  }

  private val number = "number" -> Num(42)
  private val string = "string" -> Str("42")

  checkError(
    "typo",
    Obj(number, "sttring" -> Str("42")),
    "Invalid field: sttring. Expected one of number, string, lst"
  )

  checkError(
    "typo2",
    Obj(string, "nummbber" -> Str("42")),
    "Invalid field: nummbber. Expected one of number, string, lst"
  )

  checkOk(
    "basic",
    Obj(
      "number" -> Num(42),
      "string" -> Str("42"),
      "lst" -> Lst(Str("43") :: Nil)
    ),
    AllTheAnnotations(42, "42", List("43"))
  )

  checkOk(
    "extraName",
    Obj("extraName" -> Num(33)),
    AllTheAnnotations(33, "string", List())
  )

  checkOk(
    "extraName2",
    Obj("extraName2" -> Num(33)),
    AllTheAnnotations(33, "string", List())
  )

  checkOk(
    "deprecatedName",
    Obj("deprecatedName" -> Num(33)),
    AllTheAnnotations(33, "string", List())
  )

  checkOk(
    "deprecatedName2",
    Obj("deprecatedName2" -> Num(33)),
    AllTheAnnotations(33, "string", List())
  )

  case class OneParam(param: Int)
  object OneParam {
    implicit val surface: Surface[OneParam] = Macros.deriveSurface[OneParam]
  }
  test("one param") {
    val decoder = Macros.deriveConfDecoder[OneParam](OneParam(42))
    val obtained = decoder.read(Obj("param" -> Num(2))).get
    val expected = OneParam(2)
    assert(obtained == expected)
  }

  case class Curry(a: Int)(b: String)
  object Curry {
    implicit val surface: Surface[Curry] = Macros.deriveSurface[Curry]
  }
  case class NoCurry(a: Int)
  object NoCurry {
    implicit val surface: Surface[NoCurry] = Macros.deriveSurface[NoCurry]
  }

  test("compile error") {
    assertCompiles("""Macros.deriveConfDecoder[NoCurry](NoCurry(1))""")
    assertDoesNotCompile("""Macros.deriveConfDecoder[Curry](Curry(1)("")""")
  }

  case class MissingSurface(b: Int)
  test("missing surface ") {
    assertDoesNotCompile(
      """Macros.deriveConfDecoder[MissingSurface](MissingSurface(1))"""
    )
    assertCompiles(
      """{
        |  implicit val surface = Macros.deriveSurface[MissingSurface]
        |  Macros.deriveConfDecoder[MissingSurface](MissingSurface(1))
        |}""".stripMargin
    )
  }
}
