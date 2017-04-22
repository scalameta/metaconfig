package metaconfig

import scala.language.dynamics

class ConfDynamic(val asConf: Configured[Conf]) extends Dynamic {
  def selectDynamic(name: String): ConfDynamic = {
    val result =
      asConf.flatMap {
        case obj @ Conf.Obj(values) =>
          values
            .collectFirst {
              case (`name`, value) =>
                Configured.Ok(value)
            }
            .getOrElse(ConfError.missingField(obj, name).notOk)
        case els =>
          ConfError.typeMismatch(s"Conf.Obj (with field $name)", els).notOk
      }
    ConfDynamic(result)
  }
}

object ConfDynamic {
  def apply(conf: Configured[Conf]): ConfDynamic = new ConfDynamic(conf)
}
