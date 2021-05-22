package metaconfig.cli

import munit.FunSuite
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import org.typelevel.paiges.Doc
import metaconfig.annotation.Section
import metaconfig.annotation.Description
import metaconfig.annotation.Inline
import metaconfig.annotation.Hidden
import metaconfig.ConfCodec
import metaconfig.generic.Surface

class SubcommandSuite extends FunSuite {
  case class CommonOptions(
      beep: Boolean = false
  )
  case class TestOptions(
      verbose: Boolean = false,
      @Description("The maximum number of files to test")
      maxCount: Int = 0,
      @Section("Advanced")
      magicNumber: Int = 42,
      @Hidden()
      invisible: Int = 41,
      @Inline() common: CommonOptions = CommonOptions()
  )
  implicit val commonSurface: Surface[CommonOptions] =
    metaconfig.generic.deriveSurface[CommonOptions]
  implicit val commonCodec: ConfCodec[CommonOptions] =
    metaconfig.generic.deriveCodec[CommonOptions](CommonOptions())
  implicit val surface: Surface[TestOptions] =
    metaconfig.generic.deriveSurface[TestOptions]
  implicit val codec: ConfCodec[TestOptions] =
    metaconfig.generic.deriveCodec[TestOptions](TestOptions())
  object TestCommand extends Command[TestOptions]("test") {
    override def description: Doc = Doc.paragraph("Run tests")
    override def options: Doc = Messages.options(TestOptions())
    override def usage: Doc = Doc.text("app test [OPTIONS] [project ...]")
    override def examples: Doc =
      Doc.text("app test --max-count=100 project-name")
    def run(value: Value, app: CliApp): Int = {
      app.out.println("verbose: " + value.verbose)
      app.out.println("max-count: " + value.maxCount)
      app.out.println("beep: " + value.common.beep)
      0
    }
  }
  def checkError(args: List[String], expected: String): Unit =
    check(args, expected, expectedExit = 1)
  def check(
      args: List[String],
      expected: String,
      expectedExit: Int = 0
  )(implicit loc: munit.Location): Unit = {
    test(args.mkString(" ")) {
      val out = new ByteArrayOutputStream()
      val print = new PrintStream(out)
      val app = CliApp(
        version = "1.0",
        arguments = args,
        binaryName = "app",
        commands = List(
          new HelpCommand(
            80,
            appUsage = app => Doc.text(s"${app.binaryName} COMMAND [OPTIONS]"),
            appDescription = app => Doc.text("This is a description"),
            appExamples =
              app => Doc.text(s"${app.binaryName} test --max-count=100")
          ),
          VersionCommand,
          TestCommand
        ),
        out = print,
        err = print
      )
      val exit = app.run(args)
      assertEquals(exit, expectedExit, out.toString())
      val obtained = out.toString(StandardCharsets.UTF_8.name)
      assertNoDiff(obtained, expected)
    }
  }

  check(
    List("help"),
    """|USAGE:
       |  app COMMAND [OPTIONS]
       |
       |DESCRIPTION:
       |  This is a description
       |
       |COMMANDS:
       |  help     Print this help message
       |  version  Show version information
       |  test     Run tests
       |See 'app help COMMAND' for more information on a specific command.
       |
       |EXAMPLES:
       |  app test --max-count=100
       |""".stripMargin
  )

  checkError(
    List("help", "version", "help"),
    """|error: expected 1 argument but obtained 2 arguments 'version help'
       |""".stripMargin
  )

  check(
    List("help", "version"),
    """|DESCRIPTION:
       |  Show version information
       |""".stripMargin
  )

  check(
    List("help", "test"),
    """|USAGE:
       |  app test [OPTIONS] [project ...]
       |
       |DESCRIPTION:
       |  Run tests
       |
       |OPTIONS:
       |  --verbose
       |  --max-count Int (default: 0)
       |    The maximum number of files to test
       |
       |  Advanced:
       |  --magic-number Int (default: 42)
       |  --beep
       |
       |EXAMPLES:
       |  app test --max-count=100 project-name
       |""".stripMargin
  )

  check(
    List("version"),
    """|1.0
       |""".stripMargin
  )

  checkError(
    List("unknown"),
    """|error: no such subcommand 'unknown'.
       |	Try 'app help' for more information.
       |""".stripMargin
  )

  checkError(
    List("versionn"),
    """|error: no such subcommand 'versionn'.
       |	Did you mean 'app version'?
       |	Try 'app help' for more information.
       |""".stripMargin
  )

  checkError(
    List("test", "--max=40"),
    """|error: found argument '--max' which wasn't expected, or isn't valid in this context.
       |""".stripMargin
  )
  checkError(
    List("test", "--banana=40"),
    """|error: found argument '--banana' which wasn't expected, or isn't valid in this context.
       |""".stripMargin
  )

  check(
    List("test", "--max-count", "40"),
    """|verbose: false
       |max-count: 40
       |beep: false
       |""".stripMargin
  )

  check(
    List("test", "--max-count=41", "--verbose"),
    """|verbose: true
       |max-count: 41
       |beep: false
       |""".stripMargin
  )

  List(
    List("test", "--beep"),
    List("test", "--beep=true"),
    List("test", "--common.beep"),
    List("test", "--common.beep=true")
  ).foreach { beep =>
    check(
      beep,
      """|verbose: false
         |max-count: 0
         |beep: true
         |""".stripMargin
    )
  }

}
