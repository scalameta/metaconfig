package metaconfig.cli

import metaconfig._
import metaconfig.generic.Surface

case class Site(
    foo: String = "foo",
    custom: Map[String, String] = Map.empty
)
object Site {
  implicit val surface: Surface[Site] = generic.deriveSurface[Site]
  implicit val codec: ConfCodec[Site] = generic.deriveCodec[Site](Site())
}
