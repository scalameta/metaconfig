package metaconfig.internal

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfDecoderExT
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.generic.Settings

object NoTyposDecoder {

  def apply[A: Settings](underlying: ConfDecoder[A]): ConfDecoder[A] =
    if (underlying.isInstanceOf[NoTyposDecoder[_]]) underlying
    else new NoTyposDecoder[A](underlying)

  private[internal] def checkTypos[A](conf: Conf, otherwise: => Configured[A])(
      implicit ev: Settings[A]
  ): Configured[A] =
    ConfDecoder.readWithPartial("Object") {
      case Conf.Obj(values) =>
        val names = ev.allNames
        val typos = values.collect {
          case (key, obj) if !names.contains(key) =>
            key -> obj.pos
        }
        if (typos.isEmpty) otherwise
        else ConfError.invalidFields(typos, ev.settings.map(_.name)).notOk
    }(conf)

}

class NoTyposDecoder[A: Settings](underlying: ConfDecoder[A])
    extends ConfDecoder[A] {

  override def read(conf: Conf): Configured[A] =
    NoTyposDecoder.checkTypos(conf, underlying.read(conf))

}

class NoTyposDecoderEx[S, A: Settings](underlying: ConfDecoderExT[S, A])
    extends ConfDecoderExT[S, A] {

  override def read(state: Option[S], conf: Conf): Configured[A] =
    NoTyposDecoder.checkTypos(conf, underlying.read(state, conf))

}
