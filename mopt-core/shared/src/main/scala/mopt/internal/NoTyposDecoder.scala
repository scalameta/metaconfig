package mopt.internal

import mopt.Conf
import mopt.ConfDecoder
import mopt.ConfError
import mopt.Configured
import mopt.generic.Settings
import mopt.DecoderContext

object NoTyposDecoder {
  def apply[A](
      underlying: ConfDecoder[A]
  )(implicit ev: Settings[A]): ConfDecoder[A] =
    new ConfDecoder[A] {
      override def read(conf: DecoderContext): Configured[A] = conf.conf match {
        case Conf.Obj(values) =>
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

}
