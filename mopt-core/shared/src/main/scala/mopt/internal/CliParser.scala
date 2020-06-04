package mopt.internal

import mopt._
import mopt.generic.Setting
import mopt.generic.Settings
import mopt.Configured.ok
import mopt.annotation.Inline
import mopt.Configured.NotOk

class CliParser[T](
    args: List[String],
    settings: Settings[T],
    toInline: Map[String, Setting]
) {
  import CliParser._
  def loop(
      curr: Conf.Obj,
      xs: List[String],
      s: State
  ): Configured[Conf.Obj] = {
    (xs, s) match {
      case (Nil, NoFlag) => ok(curr)
      case (Nil, Flag(flag, setting)) =>
        if (setting.isBoolean) ok(add(curr, flag, Conf.fromBoolean(true)))
        else {
          ConfError
            .message(
              s"the argument '--${Case.camelToKebab(flag)}' requires a value but none was supplied"
            )
            .notOk
        }
      case (head :: tail, NoFlag) =>
        val equal = head.indexOf('=')
        if (equal >= 0) { // split "--key=value" into ["--key", "value"]
          val key = head.substring(0, equal)
          val value = head.substring(equal + 1)
          loop(curr, key :: value :: tail, NoFlag)
        } else if (head.startsWith("-")) {
          tryFlag(curr, head, tail, s, defaultBooleanValue = true) match {
            case nok: NotOk if head.startsWith("--") =>
              val fallbackFlag =
                if (head.startsWith(noPrefix)) {
                  "--" + head.stripPrefix(noPrefix)
                } else {
                  noPrefix + head.stripPrefix("--")
                }
              val fallback =
                tryFlag(
                  curr,
                  fallbackFlag,
                  tail,
                  s,
                  defaultBooleanValue = false
                )
              fallback.orElse(nok)
            case ok => ok
          }
        } else {
          val positionalArgs =
            appendValues(
              curr,
              PositionalArgument,
              List(Conf.fromString(head))
            )
          loop(add(curr, PositionalArgument, positionalArgs), tail, NoFlag)
        }
      case (head :: tail, Flag(flag, setting)) =>
        val value = Conf.fromString(head)
        val newCurr =
          if (setting.isRepeated) {
            appendValues(curr, flag, List(value))
          } else {
            value
          }
        loop(add(curr, flag, newCurr), tail, NoFlag)
    }
  }

  private def tryFlag(
      curr: Conf.Obj,
      head: String,
      tail: List[String],
      s: State,
      defaultBooleanValue: Boolean
  ): Configured[Conf.Obj] = {
    val camel = Case.kebabToCamel(dash.replaceFirstIn(head, ""))
    camel.split("\\.").toList match {
      case Nil =>
        ConfError.message(s"Flag '$head' must not be empty").notOk
      case flag :: flags =>
        val (key, keys) = toInline.get(flag) match {
          case Some(setting) => setting.name -> (flag :: flags)
          case _ => flag -> flags
        }
        settings.get(key, keys) match {
          case None =>
            settings.settings.find(_.isCatchInvalidFlags) match {
              case None =>
                val closestCandidate =
                  Levenshtein.closestCandidate(camel, settings.names)
                val didYouMean = closestCandidate match {
                  case None =>
                    ""
                  case Some(candidate) =>
                    val kebab = Case.camelToKebab(candidate)
                    s"\n\tDid you mean '--$kebab'?"
                }
                ConfError
                  .message(
                    s"found argument '--$flag' which wasn't expected, or isn't valid in this context.$didYouMean"
                  )
                  .notOk
              case Some(fallback) =>
                val values = appendValues(
                  curr,
                  PositionalArgument,
                  (head :: tail).map(Conf.fromString)
                )
                ok(add(curr, PositionalArgument, values))
            }
          case Some(setting) =>
            val prefix = toInline.get(flag).fold("")(_.name + ".")
            val toAdd = prefix + camel
            if (setting.isBoolean) {
              val newCurr =
                add(curr, toAdd, Conf.fromBoolean(defaultBooleanValue))
              loop(newCurr, tail, NoFlag)
            } else {
              loop(curr, tail, Flag(toAdd, setting))
            }
        }
    }
  }
}

object CliParser {
  val PositionalArgument = "remainingArgs"

  def parseArgs[T](
      args: List[String]
  )(implicit settings: Settings[T]): Configured[Conf] = {
    val toInline = inlinedSettings(settings)
    val parser = new CliParser[T](args, settings, toInline)
    parser.loop(Conf.Obj(), args, NoFlag).map(_.normalize)
  }

  private def add(curr: Conf.Obj, key: String, value: Conf): Conf.Obj = {
    val values = curr.values.filterNot {
      case (k, _) => k == key
    }
    Conf.Obj((key, value) :: values)
  }

  val noPrefix = "--no-"
  def isNegatedBoolean(flag: String): Boolean = flag.startsWith(noPrefix)

  def appendValues(obj: Conf.Obj, key: String, values: List[Conf]): Conf.Lst = {
    obj.map.get(key) match {
      case Some(Conf.Lst(oldValues)) => Conf.Lst(oldValues ++ values)
      case _ => Conf.Lst(values)
    }
  }

  def inlinedSettings(settings: Settings[_]): Map[String, Setting] =
    settings.settings.iterator.flatMap { setting =>
      if (setting.annotations.exists(_.isInstanceOf[Inline])) {
        for {
          underlying <- setting.underlying.toList
          name <- underlying.names
        } yield name -> setting
      } else {
        Nil
      }
    }.toMap

  def allSettings(settings: Settings[_]): Map[String, Setting] =
    inlinedSettings(settings) ++ settings.settings.map(s => s.name -> s)

  private sealed trait State
  private case class Flag(flag: String, setting: Setting) extends State
  private case object NoFlag extends State
  private val dash = "--?".r

}
