package metaconfig.cli
import metaconfig.annotation._
import org.typelevel.paiges.Doc
import metaconfig.generic.Surface
import metaconfig.ConfCodec

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

object EchoCommand extends Command[EchoOptions]("echo") {
  override def options: Doc = Messages.options(EchoOptions.default)
  def run(value: Value, app: CliApp): Int = {
    0
  }
}

object EchoOptions {

  val default = EchoOptions()
  implicit val surface: Surface[EchoOptions] =
    metaconfig.generic.deriveSurface[EchoOptions]
  implicit val codec: ConfCodec[EchoOptions] =
    metaconfig.generic.deriveCodec[EchoOptions](default)
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
