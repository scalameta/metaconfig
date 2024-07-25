package metaconfig.cli

import metaconfig.generic
import metaconfig.annotation.Description
import metaconfig.ConfCodec
import metaconfig.generic.Surface

case class Markdownish(
    @Description(
      """|The JVM classpath is a list of path ':' separated files.
        |Example:
        |
        |```
        |a.jar:b.jar:c.jar
        |```
        |
        |The JVM classpath is a list of path ':' separated files.
        |""".stripMargin
    )
    classpath: List[String] = Nil
)
object Markdownish {
  val default: Markdownish = Markdownish()
  implicit val surface: Surface[Markdownish] =
    generic.deriveSurface[Markdownish]
  implicit val codec: ConfCodec[Markdownish] =
    generic.deriveCodec[Markdownish](default)
}
