package metaconfig.cli

import java.nio.file.Paths
import metaconfig.annotation._
import metaconfig._
import metaconfig.generic.Settings
import java.io.File

case class Site(
    foo: String = "foo",
    custom: Map[String, String] = Map.empty
)
object Site {
  implicit val surface = generic.deriveSurface[Site]
  implicit val codec = generic.deriveCodec[Site](Site())
}
