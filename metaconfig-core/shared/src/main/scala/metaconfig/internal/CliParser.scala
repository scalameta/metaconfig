package metaconfig.internal

import metaconfig._
import metaconfig.generic.Settings
import metaconfig.Configured.ok

object CliParser {
  def parseArgs[T](args: List[String])(
      implicit settings: Settings[T]): Configured[Conf] = {
    def loop(curr: Conf.Obj, xs: List[String], s: State): Configured[Conf.Obj] = {
      def add(key: String, value: Conf) = Conf.Obj((key, value) :: curr.values)
      (xs, s) match {
        case (Nil, NoFlag) => ok(curr)
        case (Nil, Flag(flag)) => ok(add(flag, Conf.fromBoolean(true)))
        case (head :: tail, NoFlag) =>
          if (head.startsWith("-")) {
            val camel = Case.kebabToCamel(dash.replaceFirstIn(head, ""))
            camel.split("\\.").toList match {
              case Nil =>
                ConfError.message(s"Flag '$head' must not be empty").notOk
              case flag :: flags =>
                settings.get(flag, flags) match {
                  case None =>
                    ConfError.invalidFields(camel :: Nil, settings.names).notOk
                  case Some(setting) =>
                    if (setting.isBoolean) {
                      val newCurr = add(camel, Conf.fromBoolean(true))
                      loop(newCurr, tail, NoFlag)
                    } else {
                      loop(curr, tail, Flag(camel))
                    }
                }
            }
          } else {
            ok(add("remainingArgs", Conf.fromList(xs.map(Conf.fromString))))
          }
        case (head :: tail, Flag(flag)) =>
          val newCurr = add(flag, Conf.fromNumberOrString(head))
          loop(newCurr, tail, NoFlag)
      }
    }
    loop(Conf.Obj(), args, NoFlag).map(_.normalize)
  }

  private sealed trait State
  private case class Flag(flag: String) extends State
  private case object NoFlag extends State
  private val dash = "--?".r

}
