package metaconfig.internal

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.generic.Settings

object NoTyposDecoder {

  def apply[A: Settings](underlying: ConfDecoder[A]): ConfDecoder[A] =
    if (underlying.isInstanceOf[NoTyposDecoder[_]]) underlying
    else new NoTyposDecoder[A](underlying)

  def checkTypos[A](conf: Conf, otherwise: => Configured[A])(
      implicit ev: Settings[A]
  ): Configured[A] =
    ConfDecoder.readWithPartial("Object") {
      case Conf.Obj(values) =>
        val names = ev.allNames
        val typos = values.collect {
          case (key, obj) if !names.contains(key) =>
            key -> obj.pos
        }
        ConfError
          .invalidFieldsOpt(typos, ev.nonHiddenNames)
          .fold(otherwise)(_.notOk)
    }(conf)

}

class NoTyposDecoder[A: Settings](underlying: ConfDecoder[A])
    extends ConfDecoder[A] {

  override def read(conf: Conf): Configured[A] =
    NoTyposDecoder.checkTypos(conf, underlying.read(conf))

}
