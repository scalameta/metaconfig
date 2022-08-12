package metaconfig

import scala.language.dynamics

class ConfDynamic(val asConf: Configured[Conf]) extends Dynamic {
  def as[T](implicit ev: ConfDecoder[T]): Configured[T] =
    asConf.andThen(_.as[T])
  def selectDynamic(name: String): ConfDynamic = {
    val result = asConf.andThen(_.getConf(name))
    ConfDynamic(result)
  }
}

object ConfDynamic {
  def apply(conf: Configured[Conf]): ConfDynamic = new ConfDynamic(conf)
}
