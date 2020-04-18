package metaconfig.tests

import metaconfig.cli.Command
import java.nio.file.Path
import java.nio.file.Paths
import metaconfig.cli.CliApp
import metaconfig.cli.HelpCommand
import metaconfig.cli.VersionCommand
import metaconfig.cli.TabCompleteCommand
import metaconfig.annotation.ExtraName
import metaconfig.ConfCodec
import metaconfig.generic.Surface

case class ExampleOptions(
    path: Path = Paths.get(".").toAbsolutePath().normalize(),
    intellij: Boolean = false,
    @ExtraName("remainingArgs")
    arguments: List[String] = Nil
)

object ExampleOptions {
  implicit val surface: Surface[ExampleOptions] =
    metaconfig.generic.deriveSurface[ExampleOptions]
  implicit val codec: ConfCodec[ExampleOptions] =
    metaconfig.generic.deriveCodec[ExampleOptions](ExampleOptions())
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
