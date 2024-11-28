package metaconfig.cli

import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import metaconfig.annotation._
import metaconfig.generic.Surface

@BinaryName("echo") @Usage("echo [OPTIONS] [ARGUMENTS ...]")
@ExampleUsage(
  """|$ echo Hello world!
     |Hello world!
     |$ echo --uppercase Hello world!
     |HELLO WORLD!
     |""".stripMargin,
)
final case class EchoOptions(
    @Description("Print out additional information.")
    verbose: Boolean = false,
    @Description("Print the message in all uppercase.")
    uppercase: Boolean = false,
)

object EchoOptions {

  val default: EchoOptions = EchoOptions()
  implicit lazy val surface: Surface[EchoOptions] = metaconfig.generic
    .deriveSurface[EchoOptions]
  implicit lazy val decoder: ConfDecoder[EchoOptions] = metaconfig.generic
    .deriveDecoder[EchoOptions](default).noTypos
  implicit lazy val encoder: ConfEncoder[EchoOptions] = metaconfig.generic
    .deriveEncoder[EchoOptions]
  def main(args: Array[String]): Unit = {
    val app = CliApp("1.0.0", "echo", commands = List(HelpCommand, EchoCommand))
    app.run(args.toList)
  }
}
