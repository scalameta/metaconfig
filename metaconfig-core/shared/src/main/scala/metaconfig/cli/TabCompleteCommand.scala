package metaconfig.cli

import scala.collection.immutable.Nil
import metaconfig.internal.CliParser
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import metaconfig.internal.Case
import java.nio.file.Path

object TabCompleteCommand extends Command[TabCompleteOptions]("tab-complete") {

  override def isHidden: Boolean = true
  def run(options: TabCompleteOptions, app: CliApp): Int = {
    val isMissingTrailingEmptyString =
      options.current.contains(options.arguments.length + 1)
    val arguments =
      if (isMissingTrailingEmptyString) options.arguments :+ ""
      else options.arguments
    arguments match {
      case _ :: _ :: Nil =>
        renderCompletions(app.commands.filterNot(_.isHidden).map(_.name), app)
      case _ :: subcommandName :: head :: tail =>
        app.commands.find(_.matchesName(subcommandName)).foreach { subcommand =>
          renderSubcommandCompletions(subcommand, head, tail, options, app)
        }
      case _ =>
    }
    0
  }

  private def renderSubcommandCompletions(
      subcommand: Command[_],
      head: String,
      tail: List[String],
      options: TabCompleteOptions,
      app: CliApp
  ): Unit = {
    val last = tail.lastOption.getOrElse(head)
    val inlined = CliParser
      .allSettings(subcommand.settings)
      .filter(!_._2.isHidden)
    val secondLast = (head :: tail).takeRight(2) match {
      case flag :: last :: Nil => Some(flag)
      case _ => None
    }
    val setting = secondLast.flatMap(flag =>
      inlined.get(Case.kebabToCamel(flag.stripPrefix("--")))
    )
    val context = TabCompletionContext(
      options.format,
      options.current,
      head :: tail,
      last,
      secondLast,
      setting,
      inlined,
      app
    )
    tabCompletions(subcommand, context).foreach { item =>
      renderCompletion(item, app)
    }
  }
  private def renderCompletions(items: List[String], app: CliApp): Unit = {
    items.foreach { item => renderCompletion(TabCompletionItem(item), app) }
  }

  private def renderCompletion(item: TabCompletionItem, app: CliApp): Unit = {
    app.out.println(item.name)
  }

  private def tabCompletions(
      command: Command[_],
      context: TabCompletionContext
  ): List[TabCompletionItem] = {
    if (context.last.startsWith("-")) {
      tabCompleteFlags(context)
    } else {
      if (context.setting.exists(_.isTabCompleteAsPath)) {
        tabCompletePath(context)
      } else {
        context.setting.flatMap(_.tabCompleteOneOf) match {
          case Some(oneof) =>
            oneof.map(TabCompletionItem(_))
          case None =>
            val fromCommand = command.complete(context)
            if (fromCommand.isEmpty && context.last == "") {
              tabCompleteFlags(context)
            } else {
              fromCommand
            }
        }
      }
    }
  }

  private def tabCompleteFlags(
      context: TabCompletionContext
  ): List[TabCompletionItem] = {
    context.allSettings
      .filterNot {
        case (_, setting) => setting.isPositionalArgument
      }
      .keys
      .toList
      .sorted
      .map(camel => TabCompletionItem("--" + Case.camelToKebab(camel)))
  }

  private def tabCompletePath(
      context: TabCompletionContext
  ): List[TabCompletionItem] = {
    val pathOrDirectory = Paths.get(context.last)
    val absolutePathOrDirectory =
      if (pathOrDirectory.isAbsolute()) pathOrDirectory
      else context.app.workingDirectory.resolve(pathOrDirectory)
    val path: Path =
      if (context.last.endsWith(File.separator)) {
        absolutePathOrDirectory
      } else if (context.last.isEmpty()) {
        absolutePathOrDirectory
      } else {
        Option(absolutePathOrDirectory.getParent())
          .getOrElse(absolutePathOrDirectory)
      }
    if (Files.isDirectory(path)) {
      path
        .toFile()
        .listFiles()
        .iterator
        .map(_.toPath())
        .map { p =>
          val slash = if (Files.isDirectory(p)) File.separator else ""
          val prefix =
            if (pathOrDirectory.isAbsolute()) {
              p.toString()
            } else {
              context.app.workingDirectory.relativize(p).toString()
            }
          prefix + slash
        }
        .map(TabCompletionItem(_))
        .toList
        .sortBy(_.name)
    } else {
      Nil
    }
  }
}
