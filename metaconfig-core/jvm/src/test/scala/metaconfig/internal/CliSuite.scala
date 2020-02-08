package metaconfig.internal

import metaconfig.generic
import metaconfig.generic.Settings
import metaconfig.Conf
import org.typelevel.paiges.Doc
import metaconfig.annotation.Description
import java.io.File
import metaconfig.HelpMessage
import metaconfig.ConfCodec
import metaconfig.generic.Surface

case class Markdownish(
    @Description(
      s"""|The JVM classpath is a list of path '${File.pathSeparator}' separated files.
          |Example:
          |
          |```
          |a.jar:b.jar:c.jar
          |```
          |
          |The JVM classpath is a list of path '${File.pathSeparator}' separated files.
          |""".stripMargin
    )
    classpath: List[String] = Nil
)
object Markdownish {
  val default = Markdownish()
  implicit val surface: Surface[Markdownish] =
    generic.deriveSurface[Markdownish]
  implicit val codec: ConfCodec[Markdownish] =
    generic.deriveCodec[Markdownish](default)
}

class CliSuite extends munit.FunSuite {
  def checkOptions[T](
      name: String,
      help: HelpMessage[T],
      expected: String
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      val obtained = help.options(80)
      assertNoDiff(obtained, expected)
    }
  }
  checkOptions(
    "markdownish",
    new HelpMessage[Markdownish](
      Markdownish(),
      "usage: markdownish",
      "version: markdownish",
      Doc.text("Use classpath!")
    ),
    """|  --classpath List[String] (default: [])
       |    The JVM classpath is a list of path ':' separated files. Example:
       |
       |    ```
       |    a.jar:b.jar:c.jar
       |    ```
       |     The JVM classpath is a list of path ':' separated files.
       |""".stripMargin
  )

  checkOptions(
    "help",
    new HelpMessage[Options](
      Options(cwd = "/tmp"),
      "usage",
      "version",
      Doc.text("description")
    ),
    """|  --in | -i String (default: "docs")
       |    The input directory to generate the fox site.
       |  --out | -o String (default: "target/fox")
       |    The output directory to generate the fox site.
       |  --cwd String (default: "/tmp")
       |  --repo-name String (default: "olafurpg/fox")
       |  --repo-url String (default: "https://github.com/olafurpg/fox")
       |  --title String (default: "Fox")
       |  --description String (default: "My Description")
       |  --google-analytics List[String] (default: [])
       |  --classpath List[String] (default: [])
       |  --clean-target
       |  --base-url String (default: "")
       |  --encoding String (default: "UTF-8")
       |
       |Advanced:
       |  --config-path String (default: "fox.conf")
       |  --remaining-args List[String] (default: [])
       |  --conf Conf (default: {})
       |  --site Site (default: {"foo": "foo", "custom": {}})
       |  --foo String (default: "foo")
       |  --custom Map[String, String] (default: {})
       |""".stripMargin
  )

}
