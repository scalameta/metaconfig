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

}

class NoTyposDecoder[A](underlying: ConfDecoder[A])(implicit ev: Settings[A])
    extends ConfDecoder[A] {

  def read(conf: Conf): Configured[A] =
    ConfDecoder.readWithPartial("Object") {
      case conf @ Conf.Obj(values) =>
        val names = ev.allNames
        val typos = values.collect {
          case (key, obj) if !names.contains(key) =>
            key -> obj.pos
        }
        if (typos.isEmpty) underlying.read(conf)
        else ConfError.invalidFields(typos, ev.settings.map(_.name)).notOk
    }(conf)
}
