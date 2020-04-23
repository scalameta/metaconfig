package metaconfig.cli

import org.typelevel.paiges.Doc

class CliSuite extends munit.FunSuite {
  def checkOptions[T](
      name: String,
      help: Doc,
      expected: String
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      val obtained = help.renderTrim(80)
      assertNoDiff(obtained, expected)
    }
  }
  checkOptions(
    "markdownish",
    Messages.options(Markdownish()),
    """|--classpath List[String] (default: [])
       |  The JVM classpath is a list of path ':' separated files. Example:
       |
       |  ```
       |  a.jar:b.jar:c.jar
       |  ```
       |   The JVM classpath is a list of path ':' separated files.
       |""".stripMargin
  )

  checkOptions(
    "help",
    Messages.options(Options(cwd = "/tmp", out = "fox")),
    """|--in | -i String (default: "docs")
       |  The input directory to generate the fox site.
       |--out | -o String (default: "fox")
       |  The output directory to generate the fox site.
       |--cwd String (default: "/tmp")
       |--repo-name String (default: "olafurpg/fox")
       |--repo-url String (default: "https://github.com/olafurpg/fox")
       |--title String (default: "Fox")
       |--description String (default: "My Description")
       |--google-analytics List[String] (default: [])
       |--classpath List[String] (default: [])
       |--clean-target
       |--default-true
       |--default-false
       |--no-flip-default-true
       |--no-flip-default-false
       |--conflict
       |--no-conflict
       |--base-url String (default: "")
       |--encoding String (default: "UTF-8")
       |
       |Advanced:
       |--config-path String (default: "fox.conf")
       |--remaining-args List[String] (default: [])
       |--conf Conf (default: {})
       |--site Site (default: {"foo": "foo", "custom": {}})
       |--foo String (default: "foo")
       |--custom Map[String, String] (default: {})
       |""".stripMargin
  )

}
