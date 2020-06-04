package mopt.tests

import mopt.cli.Command
import java.nio.file.Path
import java.nio.file.Paths
import mopt.cli.CliApp
import mopt.cli.HelpCommand
import mopt.cli.VersionCommand
import mopt.cli.TabCompleteCommand
import mopt.annotation.ExtraName
import mopt.ConfCodec
import mopt.generic.Surface

case class ExampleOptions(
    path: Path = Paths.get(".").toAbsolutePath().normalize(),
    intellij: Boolean = false,
    @ExtraName("remainingArgs")
    arguments: List[String] = Nil
)

object ExampleOptions {
  implicit val surface: Surface[ExampleOptions] =
    mopt.generic.deriveSurface[ExampleOptions]
  implicit val codec: ConfCodec[ExampleOptions] =
    mopt.generic.deriveCodec[ExampleOptions](ExampleOptions())
}

object ExampleCommand extends Command[ExampleOptions]("example") {
  def run(value: Value, app: CliApp): Int = {
    0
  }
}

object ExampleMain {
  val app: CliApp = CliApp(
    version = "1.0",
    "hello",
    List(
      HelpCommand,
      VersionCommand,
      ExampleCommand,
      TabCompleteCommand
    )
  )
  def main(args: Array[String]): Unit = {
    val exit = app.run(args.toList)
    System.exit(exit)
  }
}
