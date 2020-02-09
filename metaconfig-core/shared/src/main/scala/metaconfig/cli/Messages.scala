package metaconfig.cli

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import metaconfig.annotation.Hidden
import metaconfig.annotation.Inline
import metaconfig.annotation.Section
import org.typelevel.paiges.Doc
import scala.collection.mutable.ListBuffer
import metaconfig.internal.Case
import metaconfig.Conf
import metaconfig.ConfEncoder
import metaconfig.generic.Surface
import metaconfig.generic.Setting
import metaconfig.generic.Settings

object Messages {

  def options[T: Surface: ConfEncoder](default: T): Doc = {
    val settings = Settings[T]
    val obj = ConfEncoder[T].writeObj(default)
    val docs = settings.settings.zip(obj.values).flatMap {
      case (setting, (_, value)) =>
        if (setting.annotations.exists(_.isInstanceOf[Inline])) {
          for {
            underlying <- setting.underlying.toList
            (field, (_, fieldDefault)) <- underlying.settings
              .zip(value.asInstanceOf[Conf.Obj].values)
          } yield {
            printOption(field, fieldDefault)
          }
        } else {
          List(printOption(setting, value))
        }
    }
    Doc.intercalate(Doc.line, docs)
  }

  private def printOption(setting: Setting, value: Conf): Doc = {
    if (setting.annotations.exists(_.isInstanceOf[Hidden])) Doc.empty
    else {
      var doc = Doc.empty
      setting.annotations.foreach {
        case section: Section =>
          doc += Doc.line + Doc.text(section.name) + Doc.char(':') + Doc.line
        case _ =>
      }
      val name = Case.camelToKebab(setting.name)
      doc += Doc.text("--") + Doc.text(name)
      setting.extraNames.foreach { name =>
        if (name.length == 1) {
          doc += Doc.text(" | -") +
            Doc.text(Case.camelToKebab(name))
        }
      }
      if (!setting.isBoolean) {
        doc += Doc.space +
          Doc.text(setting.tpe) +
          Doc.text(" (default: ") +
          Doc.text(value.toString()) +
          Doc.text(")")
      }
      setting.description
        .filter(_.nonEmpty)
        .foreach { description =>
          doc += Doc.line +
            Messages.markdownish(description).indent(2)
        }
      doc
    }
  }

  /** Line wrap prose while keeping markdown code fences unchanged. */
  def markdownish(text: String): Doc = {
    val buf = ListBuffer.empty[String]
    val paragraphs = ListBuffer.empty[Doc]
    var insideCodeFence = false
    def flush(): Unit = {
      if (insideCodeFence) {
        paragraphs += Doc.intercalate(Doc.line, buf.map(Doc.text))
      } else {
        paragraphs += Doc.paragraph(buf.mkString("\n"))
      }
      buf.clear()
    }
    text.linesIterator.foreach { line =>
      if (line.startsWith("```")) {
        flush()
        insideCodeFence = !insideCodeFence
      } else if (line.isEmpty()) {
        flush()
      }
      buf += line
    }
    flush()
    Doc.intercalate(Doc.line, paragraphs)
  }

}
