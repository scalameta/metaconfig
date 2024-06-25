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
