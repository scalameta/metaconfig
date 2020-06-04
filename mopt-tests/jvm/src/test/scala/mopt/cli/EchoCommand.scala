package mopt.cli

import org.typelevel.paiges.Doc

object EchoCommand extends Command[EchoOptions]("echo") {
  override def options: Doc = Messages.options(EchoOptions.default)
  def run(value: Value, app: CliApp): Int = {
    0
  }
}
