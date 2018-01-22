package metaconfig

import metaconfig.Conf._
import metaconfig.annotation._
import metaconfig.generic.Settings
import metaconfig.generic.Surface
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
    generic.deriveSurface[AllTheAnnotations]
  lazy val settings = Settings[AllTheAnnotations]
  implicit lazy val decoder: ConfDecoder[AllTheAnnotations] =
    generic.deriveDecoder[AllTheAnnotations](AllTheAnnotations()).noTypos
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
    implicit val surface: Surface[OneParam] = generic.deriveSurface[OneParam]
  }
  test("one param") {
    val decoder = generic.deriveDecoder[OneParam](OneParam(42))
    val obtained = decoder.read(Obj("param" -> Num(2))).get
    val expected = OneParam(2)
    assert(obtained == expected)
  }

  case class Curry(a: Int)(b: String)
  object Curry {
    implicit val surface: Surface[Curry] = generic.deriveSurface[Curry]
  }
  case class NoCurry(a: Int)
  object NoCurry {
    implicit val surface: Surface[NoCurry] = generic.deriveSurface[NoCurry]
  }

  test("compile error") {
    assertCompiles("""generic.deriveDecoder[NoCurry](NoCurry(1))""")
    assertDoesNotCompile("""generic.deriveDecoder[Curry](Curry(1)("")""")
  }

  case class MissingSurface(b: Int)
  test("missing surface ") {
    assertDoesNotCompile(
      """generic.deriveDecoder[MissingSurface](MissingSurface(1))"""
    )
    assertCompiles(
      """{
        |  implicit val surface = generic.deriveSurface[MissingSurface]
        |  generic.deriveDecoder[MissingSurface](MissingSurface(1))
        |}""".stripMargin
    )
  }
}
