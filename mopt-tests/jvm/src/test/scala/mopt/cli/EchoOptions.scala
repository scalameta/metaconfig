package mopt.cli

import mopt.annotation._
import mopt.generic.Surface
import mopt.ConfDecoder
import mopt.ConfEncoder

@BinaryName("echo")
@Usage("echo [OPTIONS] [ARGUMENTS ...]")
@ExampleUsage(
  """|$ echo Hello world!
     |Hello world!
     |$ echo --uppercase Hello world!
     |HELLO WORLD!
     |""".stripMargin
)
final case class EchoOptions(
    @Description("Print out additional information.")
    verbose: Boolean = false,
    @Description("Print the message in all uppercase.")
    uppercase: Boolean = false
)

object EchoOptions {

  val default: EchoOptions = EchoOptions()
  implicit lazy val surface: Surface[EchoOptions] =
    mopt.generic.deriveSurface[EchoOptions]
  implicit lazy val decoder: ConfDecoder[EchoOptions] =
    mopt.generic.deriveDecoder[EchoOptions](default).noTypos
  implicit lazy val encoder: ConfEncoder[EchoOptions] =
    mopt.generic.deriveEncoder[EchoOptions]
  def main(args: Array[String]): Unit = {
    val app = CliApp(
      "1.0.0",
      "echo",
      commands = List(
        HelpCommand,
        EchoCommand
      )
    )
    app.run(args.toList)
  }
}
