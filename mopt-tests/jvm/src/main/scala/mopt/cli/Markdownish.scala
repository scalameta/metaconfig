package mopt.cli

import mopt.generic
import mopt.annotation.Description
import mopt.ConfCodec
import mopt.generic.Surface

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
