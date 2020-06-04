package mopt.cli

import java.io.PrintStream
import mopt.ConfEncoder
import mopt.generic.Surface
import org.typelevel.paiges.Doc
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import mopt.generic.Settings
import mopt.ConfDecoder

abstract class Command[T](val name: String)(
    implicit val _settings: Settings[T],
    confEncoder: ConfEncoder[T],
    confDecoder: ConfDecoder[T]
) { self =>
  type Value = T

  def run(value: Value, app: CliApp): Int
  def complete(context: TabCompletionContext): List[TabCompletionItem] = Nil
  def description: Doc = settings.cliDescription.getOrElse(Doc.empty)
  def usage: Doc = settings.cliUsage.getOrElse(Doc.empty)
  def options: Doc = Doc.empty
  def examples: Doc = Doc.intercalate(Doc.line, settings.cliExamples)
  def extraNames: List[String] = Nil
  def isHidden: Boolean = false

  final def settings: Settings[Value] = _settings
  final def encoder: ConfEncoder[Value] = confEncoder
  final def decoder: ConfDecoder[Value] = confDecoder
  final def allNames: List[String] = name :: extraNames.toList
  final def matchesName(name: String): Boolean =
    allNames.contains(name)
  final def contramap[B: Surface: ConfEncoder: ConfDecoder](
      default: B,
      fn: B => T
  ): Command[B] =
    new Command[B](name) {
      def run(value: B, app: CliApp): Int = self.run(fn(value), app)
      override def description = self.description
      override def options = self.options
      override def usage = self.usage
      override def extraNames = self.extraNames
    }

  override def toString(): String = s"Command($name)"

  final def helpMessage(out: PrintStream, width: Int): Unit = {
    val docs = List[(String, Doc)](
      "USAGE:" -> usage,
      "DESCRIPTION:" -> description,
      "OPTIONS:" -> options,
      "EXAMPLES:" -> examples
    ).collect {
      case (key, doc) if doc.nonEmpty =>
        Doc.text(key) + Doc.line + doc.indent(2)
    }
    val blank = Doc.line + Doc.line
    val help = Doc.intercalate(blank, docs).renderTrim(width)
    out.println(help)
  }

  final def helpMessage(width: Int): String = {
    val baos = new ByteArrayOutputStream()
    helpMessage(new PrintStream(baos), width)
    baos.toString(StandardCharsets.UTF_8.name())
  }
}
