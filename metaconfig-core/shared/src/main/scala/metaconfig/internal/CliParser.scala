package metaconfig.internal

import metaconfig._
import metaconfig.generic.Settings
import metaconfig.Configured.ok

object CliParser {
  def parseArgs[T](args: List[String])(
      implicit settings: Settings[T]): Configured[Conf] = {
    def loop(
        curr: Conf.Obj,
        xs: List[String],
        s: State): Configured[Conf.Obj] = {
      def add(key: String, value: Conf) = Conf.Obj((key, value) :: curr.values)
      (xs, s) match {
        case (Nil, NoFlag) => ok(curr)
        case (Nil, Flag(flag)) => ok(add(flag, Conf.fromBoolean(true)))
        case (head :: tail, NoFlag) =>
          if (head.startsWith("-")) {
            val flag = Case.kebabToCamel(dash.replaceFirstIn(head, ""))
            settings.get(flag) match {
              case None =>
                ConfError.invalidFields(flag :: Nil, settings.names).notOk
              case Some(setting) =>
                if (setting.isBoolean) {
                  val newCurr = add(flag, Conf.fromBoolean(true))
                  loop(newCurr, tail, NoFlag)
                } else {
                  loop(curr, tail, Flag(flag))
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
    loop(Conf.Obj(), args, NoFlag)
  }

  private sealed trait State
  private case class Flag(flag: String) extends State
  private case object NoFlag extends State
  private val dash = "--?".r

}
