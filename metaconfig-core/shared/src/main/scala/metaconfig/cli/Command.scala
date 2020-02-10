package metaconfig.cli

import java.io.PrintStream
import java.io.InputStream
import metaconfig.generic
import metaconfig.ConfEncoder
import metaconfig.generic.Surface
import java.nio.file.Path
import org.typelevel.paiges.Doc
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import metaconfig.generic.Settings
import metaconfig.ConfDecoder

abstract class Command[T](val name: String)(
    implicit surface: Surface[T],
    confEncoder: ConfEncoder[T],
    confDecoder: ConfDecoder[T]
) { self =>
  type Value = T

  def run(value: Value, app: CliApp): Int
  def description: Doc = Doc.empty
  def options: Doc = Doc.empty
  def usage: Doc = Doc.empty
  def examples: Doc = Doc.empty
  def extraNames: List[String] = Nil

  final def settings: Settings[Value] = Settings[T]
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
        Doc.line + Doc.text(key) + Doc.line + doc.indent(2)
    }
    val help = Doc.intercalate(Doc.line, docs).renderTrim(width)
    out.println(help)
  }

  final def helpMessage(width: Int): String = {
    val baos = new ByteArrayOutputStream()
    helpMessage(new PrintStream(baos), width)
    baos.toString(StandardCharsets.UTF_8.name())
  }
}
