package metaconfig.cli

import munit.FunSuite
import munit.TestOptions
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import metaconfig.generic
import metaconfig.annotation._
import java.nio.file.Paths
import java.nio.file.Path
import metaconfig.ConfCodec
import java.nio.file.Files
import java.io.File

class TabCompletionSuite extends FunSuite {

  case class TabOptions(
      @Description("The input directory to generate the fox site.")
      @ExtraName("i")
      in: Path = Paths.get("docs"),
      out: Path = Paths.get("target").resolve("fox"),
      custom: String = "",
      @Hidden // should not appear in --help
      hidden: Int = 87
  )
  object TabOptions {
    implicit val surface: generic.Surface[TabOptions] =
      generic.deriveSurface[TabOptions]
    implicit val codec: ConfCodec[TabOptions] =
      generic.deriveCodec[TabOptions](TabOptions())
  }
  object TabCommand extends Command[TabOptions]("tab") {
    override def complete(
        context: TabCompletionContext
    ): List[TabCompletionItem] = {
      if (context.last.isEmpty()) List(TabCompletionItem("empty"))
      else {
        val setting = context.setting.map(_.name + "-").getOrElse("")
        List(
          TabCompletionItem(
            "setting:" + context.setting.fold("")(s => s" ${s.name}")
          ),
          TabCompletionItem(
            "secondLast:" + context.secondLast.fold("")(s => s" ${s}")
          ),
          TabCompletionItem(
            s"last: ${context.last}"
          )
        )
      }
    }
    def run(value: Value, app: CliApp): Int = 0
  }
  def check(
      name: TestOptions,
      userArgs: List[String],
      expected: String
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      val pwd = Files.createTempDirectory("metaconfig")
      Files.write(pwd.resolve("hello"), Array.emptyByteArray)
      Files.write(pwd.resolve("goodbye"), Array.emptyByteArray)
      val project = Files.createDirectories(pwd.resolve("project"))
      Files.write(project.resolve("inner"), Array.emptyByteArray)
      Files.write(project.resolve("outer"), Array.emptyByteArray)
      val binaryName = "app"
      val words = binaryName :: userArgs
      val args: List[String] =
        "tab-complete" ::
          "--current" :: words.length.toString ::
          "--format" :: "zsh" ::
          words
      val out = new ByteArrayOutputStream()
      val print = new PrintStream(out)
      val app = CliApp(
        version = "1.0",
        arguments = args,
        binaryName = binaryName,
        commands = List(
          HelpCommand,
          VersionCommand,
          TabCommand,
          TabCompleteCommand
        ),
        out = print,
        workingDirectory = pwd
      )
      val exit = app.run(args)
      assertEquals(exit, 0, out.toString())
      val obtained = out.toString(StandardCharsets.UTF_8.name)
      assertNoDiff(obtained, expected)
    }
  }

  check(
    "basic",
    List("h"),
    """|help
      |version
      |tab
      |""".stripMargin
  )

  check(
    "flag",
    List("tab", "-"),
    """|--custom
      |--in
      |--out
      |""".stripMargin
  )

  check(
    "path-empty",
    List("tab", "--in", ""),
    """|goodbye
      |hello
      |project/
      |""".stripMargin.replace('/', File.separatorChar)
  )

  check(
    "path-no-slash",
    List("tab", "--in", "project"),
    """|goodbye
      |hello
      |project/
      |""".stripMargin.replace('/', File.separatorChar)
  )

  check(
    "path-directory",
    List("tab", "--in", "project" + File.separatorChar),
    """|project/inner
      |project/outer
      |""".stripMargin.replace('/', File.separatorChar)
  )

  check(
    "custom-empty",
    List("tab", "--custom", ""),
    "empty"
  )

  check(
    "custom-non-empty",
    List("tab", "--custom", "abba"),
    """|setting: custom
      |secondLast: --custom
      |last: abba
      |""".stripMargin
  )

  check(
    "custom-non-empty",
    List("tab", "--unknown", "abba"),
    """|setting:
      |secondLast: --unknown
      |last: abba
      |""".stripMargin
  )

  check(
    "error-empty",
    List("--unknown", "value"),
    ""
  )

  check(
    "error-empty",
    List("--unknown", "value"),
    ""
  )

}
