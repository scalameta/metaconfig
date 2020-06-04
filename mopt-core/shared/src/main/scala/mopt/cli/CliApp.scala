package mopt.cli

import java.io.PrintStream
import java.io.InputStream
import java.nio.file.Path
import fansi.Str
import fansi.Color
import java.nio.file.Paths
import mopt.Conf
import mopt.Configured.Ok
import mopt.Configured.NotOk
import mopt.Configured

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
  def warn(message: Str): Unit = {
    err.println(Color.LightYellow("warn: ") ++ message)
  }
  def info(message: Str): Unit = {
    err.println(Color.LightBlue("info: ") ++ message)
  }

  def run(args: List[String]): Int = {
    val app = this.copy(arguments = args)
    args match {
      case Nil => onEmptyArguments.run((), app)
      case subcommand :: tail =>
        commands.find(_.matchesName(subcommand)) match {
          case Some(command) =>
            val conf = Conf.parseCliArgs[command.Value](tail)(command.settings)
            val configured: Configured[command.Value] =
              conf.andThen(_.as[command.Value](command.decoder))
            configured match {
              case Ok(value) =>
                command.run(value, app)
              case NotOk(error) =>
                error.all.foreach { message => app.error(message) }
                1
            }
          case None =>
            HelpCommand.notRecognized(subcommand, app)
        }
    }
  }
}
