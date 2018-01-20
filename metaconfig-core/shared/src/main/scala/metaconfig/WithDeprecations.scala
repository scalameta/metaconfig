package metaconfig

import io.circe.Decoder
import io.circe.HCursor


case class WithDeprecations[T](
    value: T,
    deprecations: Iterable[DeprecatedSettingName])

object WithDeprecations {
  implicit def CirceToDeprecatedDecoder[A](
      implicit decoder: Decoder[A],
      settings: Settings[A]): Decoder[WithDeprecations[A]] =
    new Decoder[WithDeprecations[A]] {
      override def apply(c: HCursor): Decoder.Result[WithDeprecations[A]] = {
        decoder.apply(c).right.map { ok =>
          val deprecations = c.keys.getOrElse(Nil).collect {
            case settings.Deprecated(d) => d
          }
          WithDeprecations(ok, deprecations)
        }
      }
    }
}
