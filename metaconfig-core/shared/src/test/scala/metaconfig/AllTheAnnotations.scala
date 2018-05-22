package metaconfig

import metaconfig.annotation.Deprecated
import metaconfig.annotation.DeprecatedName
import metaconfig.annotation.Description
import metaconfig.annotation.ExampleValue
import metaconfig.annotation.ExtraName
import metaconfig.annotation.SinceVersion
import metaconfig.generic.Surface
import org.scalacheck.Arbitrary

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
  implicit lazy val decoder: ConfDecoder[AllTheAnnotations] =
    generic.deriveDecoder[AllTheAnnotations](AllTheAnnotations()).noTypos
  implicit val encoder: ConfEncoder[AllTheAnnotations] =
    generic.deriveEncoder[AllTheAnnotations]
}

case class OneParam(param: Int)
object OneParam {
  implicit val surface: Surface[OneParam] = generic.deriveSurface[OneParam]
}

case class HasOption(b: Option[Int] = None)
object HasOption {
  implicit val surface = generic.deriveSurface[HasOption]
  implicit val decoder = generic.deriveDecoder[HasOption](HasOption())
  implicit val encoder = generic.deriveEncoder[HasOption]
}

case class Curry(a: Int)(b: String)
object Curry {
  implicit val surface: Surface[Curry] = generic.deriveSurface[Curry]
}
case class NoCurry(a: Int)
object NoCurry {
  implicit val surface: Surface[NoCurry] = generic.deriveSurface[NoCurry]
}
case class NoParam()
object NoParam {
  implicit val surface: Surface[NoParam] = generic.deriveSurface[NoParam]
  implicit val encoder: ConfEncoder[NoParam] = generic.deriveEncoder
}

case class IsIterable(
    a: Set[Int] = Set.empty,
    b: Iterable[String] = Iterable.empty
)
object IsIterable {
  implicit val surface = generic.deriveSurface[IsIterable]
  implicit val decoder = generic.deriveDecoder[IsIterable](IsIterable())
  implicit val encoder = generic.deriveEncoder[IsIterable]
}
