package metaconfig

import metaconfig.annotation.Deprecated
import metaconfig.annotation.DeprecatedName
import metaconfig.annotation.Description
import metaconfig.annotation.ExampleValue
import metaconfig.annotation.ExtraName
import metaconfig.annotation.SinceVersion
import metaconfig.generic.Surface

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
  implicit lazy val decoderEx: ConfDecoderEx[AllTheAnnotations] =
    generic.deriveDecoderEx[AllTheAnnotations](AllTheAnnotations()).noTypos
  implicit val encoder: ConfEncoder[AllTheAnnotations] =
    generic.deriveEncoder[AllTheAnnotations]
}

case class OneParam(param: Int = 82)
object OneParam {
  implicit val surface: Surface[OneParam] = generic.deriveSurface[OneParam]
  implicit val codec: ConfCodec[OneParam] =
    generic.deriveCodec[OneParam](OneParam())
  implicit val decoderEx: ConfDecoderEx[OneParam] =
    generic.deriveDecoderEx[OneParam](OneParam()).noTypos
}

case class HasOption(b: Option[Int] = None)
object HasOption {
  implicit val surface: Surface[HasOption] = generic.deriveSurface[HasOption]
  implicit val decoder: ConfDecoder[HasOption] =
    generic.deriveDecoder[HasOption](HasOption())
  implicit val decoderEx: ConfDecoderEx[HasOption] =
    generic.deriveDecoderEx[HasOption](HasOption())
  implicit val encoder: ConfEncoder[HasOption] =
    generic.deriveEncoder[HasOption]
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
  implicit val surface: Surface[IsIterable] = generic.deriveSurface[IsIterable]
  implicit val decoder: ConfDecoder[IsIterable] =
    generic.deriveDecoder[IsIterable](IsIterable())
  implicit val decoderEx: ConfDecoderEx[IsIterable] =
    generic.deriveDecoderEx[IsIterable](IsIterable()).noTypos
  implicit val encoder: ConfEncoder[IsIterable] =
    generic.deriveEncoder[IsIterable]
}

case class Nested2(
    a: String = "nested2",
    b: OneParam = OneParam(),
    c: Map[String, OneParam] = Map("k2" -> OneParam(2))
)
object Nested2 {
  implicit val surface: Surface[Nested2] = generic.deriveSurface[Nested2]
  implicit val codec: ConfCodec[Nested2] =
    generic.deriveCodec[Nested2](Nested2())
  implicit val decoderEx: ConfDecoderEx[Nested2] =
    generic.deriveDecoderEx[Nested2](Nested2()).noTypos
}

case class Nested3(
    a: String = "nested3",
    b: Nested2 = Nested2()
)
object Nested3 {
  implicit val surface: Surface[Nested3] = generic.deriveSurface[Nested3]
  implicit val codec: ConfCodec[Nested3] =
    generic.deriveCodec[Nested3](Nested3())
  implicit val decoderEx: ConfDecoderEx[Nested3] =
    generic.deriveDecoderEx[Nested3](Nested3()).noTypos
}

case class Nested(
    a: Int = 31,
    b: OneParam = OneParam(),
    c: Nested2 = Nested2(),
    d: Seq[Nested2] = Seq(Nested2("n1", OneParam(2), Map("k1" -> OneParam(1)))),
    e: Nested3 = Nested3()
)
object Nested {
  implicit val surface: Surface[Nested] = generic.deriveSurface[Nested]
  implicit val codec: ConfCodec[Nested] = generic.deriveCodec[Nested](Nested())
  implicit val decoderEx: ConfDecoderEx[Nested] =
    generic.deriveDecoderEx[Nested](Nested()).noTypos
}
