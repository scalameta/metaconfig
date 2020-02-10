package metaconfig.cli

import java.io.PrintStream
import java.io.InputStream
import metaconfig.generic
import metaconfig.ConfEncoder
import metaconfig.generic.Surface
import java.nio.file.Path
import fansi.Str
import fansi.Color
import java.nio.file.Paths
import metaconfig.Conf
import metaconfig.Configured.Ok
import metaconfig.Configured.NotOk
import metaconfig.Configured

case class CliApp(
    version: String,
    binaryName: String,
    commands: List[Command[_]],
    onEmptyArguments: Command[Unit] = HelpCommand.noSubcommand,
    arguments: List[String] = Nil,
    out: PrintStream = System.out,
    err: PrintStream = System.err,
    in: InputStream = System.in,
    workingDirectory: Path = Paths.get(System.getProperty("user.dir")),
    environmentVariables: Map[String, String] = sys.env
) {
  def error(message: Str): Unit = {
    err.println(Color.LightRed("error: ") ++ message)
  }
  def allCommands = onEmptyArguments :: commands

  def run(args: List[String]): Int = {
    val app = this.copy(arguments = args)
    args match {
      case Nil => onEmptyArguments.run((), app)
      case subcommand :: tail =>
        commands.find(_.matchesName(subcommand)) match {
          case Some(command) =>
            val configured: Configured[command.Value] = Conf
              .parseCliArgs[command.Value](tail)(command.settings)
              .andThen(_.as[command.Value](command.decoder))
            configured match {
              case Ok(value) =>
                command.run(value, app)
              case NotOk(error) =>
                error.all.foreach { message =>
                  app.error(message)
                }
                1
            }
          case None =>
            HelpCommand.notRecognized(subcommand, app)
        }
    }
  }
}
