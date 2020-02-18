package metaconfig.cli

import metaconfig.generic.Field
import metaconfig.generic.Surface
import metaconfig.ConfEncoder
import metaconfig.Conf
import org.typelevel.paiges.Doc
import scala.util.control.NonFatal
import metaconfig.Conf.Str
import java.io.PrintStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import metaconfig.internal.TermInfo
import metaconfig.annotation.ExtraName
import metaconfig.ConfDecoder
import metaconfig.Configured
import metaconfig.Conf.Obj
import metaconfig.internal.Levenshtein

object HelpCommand
    extends HelpCommand(
      screenWidth = TermInfo.screenWidth(),
      appUsage = app => Doc.text(s"${app.binaryName} COMMAND [OPTIONS]"),
      appDescription = app => Doc.empty,
      appExamples = app => Doc.empty
    )
class HelpCommand(
    screenWidth: Int,
    appUsage: CliApp => Doc,
    appDescription: CliApp => Doc,
    appExamples: CliApp => Doc
) extends Command[HelpOptions]("help") {
  override def description = Doc.paragraph("Print this help message")
  override def extraNames: List[String] = List("-h", "--help", "-help")
  override def complete(
      context: TabCompletionContext
  ): List[TabCompletionItem] = {
    if (context.arguments.length <= 1) {
      context.app.commands
        .filterNot(_.isHidden)
        .map(c => TabCompletionItem(c.name))
    } else {
      Nil
    }
  }
  def run(options: HelpOptions, app: CliApp): Int = {
    options.subcommand match {
      case Nil =>
        val usage = appUsage(app)
        if (usage.nonEmpty) {
          app.out.println(s"USAGE:")
          app.out.println(usage.indent(2).renderTrim(screenWidth))
        }
        val description = appDescription(app)
        if (description.nonEmpty) {
          if (usage.nonEmpty) app.out.println()
          app.out.println(s"DESCRIPTION:")
          app.out.println(description.indent(2).renderTrim(screenWidth))
        }
        if (app.commands.nonEmpty) {
          val rows = app.commands.map { command =>
            command.name -> command.description
          }
          val message = Doc.tabulate(' ', "  ", rows).indent(2)
          if (usage.nonEmpty) app.out.println()
          app.out.println(s"COMMANDS:")
          app.out.println(message.renderTrim(screenWidth))
          app.out.println(
            (Doc.text(s"See '${app.binaryName} help COMMAND' ") +
              Doc.paragraph(s"for more information on a specific command."))
              .renderTrim(screenWidth)
          )
        }
        val examples = appExamples(app)
        if (examples.nonEmpty) {
          if (app.commands.nonEmpty) app.out.println()
          app.out.println(s"EXAMPLES:")
          app.out.println(examples.indent(2).renderTrim(screenWidth))
        }
        0
      case subcommand :: Nil =>
        app.commands.find(_.matchesName(subcommand)) match {
          case Some(command) =>
            command.helpMessage(app.out, screenWidth)
            0
          case None =>
            notRecognized(subcommand, app)
        }
      case obtained =>
        app.error(
          s"expected 1 argument but obtained ${obtained.length} arguments " +
            obtained.mkString("'", " ", "'")
        )
        1
    }
  }

  def notRecognized(subcommand: String, app: CliApp): Int = {
    val closestSubcommand =
      Levenshtein.closestCandidate(subcommand, app.commands.map(_.name))
    val didYouMean = closestSubcommand match {
      case None => ""
      case Some(candidate) =>
        s"\n\tDid you mean '${app.binaryName} $candidate'?"
    }
    app.error(
      s"no such subcommand '$subcommand'.$didYouMean\n\tTry '${app.binaryName} help' for more information."
    )
    1
  }

  val noSubcommand: Command[Unit] =
    this.contramap[Unit]((), _ => HelpOptions())
}
