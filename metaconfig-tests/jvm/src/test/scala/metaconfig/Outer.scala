package metaconfig

import metaconfig.generic.Surface

case class Outer(inner: Inner = Inner(), c: Int = 0)
object Outer {
  implicit val surface: Surface[Outer] = generic.deriveSurface
  implicit val codec: ConfCodec[Outer] = generic.deriveCodec(Outer())
}
