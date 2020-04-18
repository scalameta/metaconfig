package metaconfig

import metaconfig.generic.Surface

case class Inner(a: String = "a", b: Boolean = true)
object Inner {
  implicit val surface: Surface[Inner] = generic.deriveSurface[Inner]
  implicit val codec: ConfCodec[Inner] = generic.deriveCodec(Inner())
}
