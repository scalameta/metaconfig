package metaconfig.internal

import metaconfig._
import metaconfig.generic.Setting
import metaconfig.generic.Settings
import metaconfig.Configured.ok
import metaconfig.annotation.Inline
import scala.collection.immutable.Nil

object CliParser {

  def parseArgs[T](
      args: List[String]
  )(implicit settings: Settings[T]): Configured[Conf] = {
    val toInline: Map[String, Setting] =
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
    def loop(
        curr: Conf.Obj,
        xs: List[String],
        s: State
    ): Configured[Conf.Obj] = {
      def add(key: String, value: Conf): Conf.Obj = {
        val values = curr.values.filterNot {
          case (k, _) => k == key
        }
        Conf.Obj((key, value) :: values)
      }

      (xs, s) match {
        case (Nil, NoFlag) => ok(curr)
        case (Nil, Flag(flag, _)) => ok(add(flag, Conf.fromBoolean(true)))
        case (head :: tail, NoFlag) =>
          val equal = head.indexOf('=')
          if (equal >= 0) { // split "--key=value" into ["--key", "value"]
            val key = head.substring(0, equal)
            val value = head.substring(equal + 1)
            loop(curr, key :: value :: tail, NoFlag)
          } else if (head.startsWith("-")) {
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
                  case Some(setting) =>
                    if (setting.isBoolean) {
                      val newCurr = add(camel, Conf.fromBoolean(true))
                      loop(newCurr, tail, NoFlag)
                    } else {
                      val prefix = toInline.get(flag).fold("")(_.name + ".")
                      loop(curr, tail, Flag(prefix + camel, setting))
                    }
                }
            }
          } else {
            ok(add("remainingArgs", Conf.fromList(xs.map(Conf.fromString))))
          }
        case (head :: tail, Flag(flag, setting)) =>
          val value = Conf.fromString(head)
          val newCurr =
            if (setting.isRepeated) {
              curr.map.get(flag) match {
                case Some(Conf.Lst(values)) => Conf.Lst(values :+ value)
                case _ => Conf.Lst(value :: Nil)
              }
            } else {
              value
            }
          loop(add(flag, newCurr), tail, NoFlag)
      }
    }
    loop(Conf.Obj(), args, NoFlag).map(_.normalize)
  }

  private sealed trait State
  private case class Flag(flag: String, setting: Setting) extends State
  private case object NoFlag extends State
  private val dash = "--?".r

}
