package metaconfig.internal

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.generic.Settings

object NoTyposDecoder {
  def apply[A](
      underlying: ConfDecoder[A]
  )(implicit ev: Settings[A]): ConfDecoder[A] =
    _ match {
      case conf @ Conf.Obj(values) =>
        val names = ev.allNames
        val typos = values.collect {
          case (key, obj) if !names.contains(key) =>
            key -> obj.pos
        }
        if (typos.isEmpty) underlying.read(conf)
        else ConfError.invalidFields(typos, ev.settings.map(_.name)).notOk
      case els =>
        ConfError.typeMismatch("Object", els).notOk
    }

}
