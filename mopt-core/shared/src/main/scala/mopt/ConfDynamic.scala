package mopt

import scala.language.dynamics

class ConfDynamic(val asConf: Configured[Conf]) extends Dynamic {
  def as[T](implicit ev: ConfDecoder[T]): Configured[T] =
    asConf.andThen(_.as[T])
  def selectDynamic(name: String): ConfDynamic = {
    val result =
      asConf.andThen {
        case obj @ Conf.Obj(values) =>
          values
            .collectFirst {
              case (`name`, value) =>
                Configured.Ok(value)
            }
            .getOrElse(ConfError.missingField(obj, name).notOk)
        case els =>
          ConfError
            .typeMismatch(s"Conf.Obj (with field $name)", els, name)
            .notOk
      }
    ConfDynamic(result)
  }
}

object ConfDynamic {
  def apply(conf: Configured[Conf]): ConfDynamic = new ConfDynamic(conf)
}
