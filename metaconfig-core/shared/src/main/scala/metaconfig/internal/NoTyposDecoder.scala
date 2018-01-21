package metaconfig.internal

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Settings

object NoTyposDecoder {
  def apply[A](underlying: ConfDecoder[A])(
      implicit ev: Settings[A]): ConfDecoder[A] =
    new ConfDecoder[A] {
      override def read(conf: Conf): Configured[A] = conf match {
        case Conf.Obj(values) =>
          val names = ev.allNames
          val typos = values.collect {
            case (key, _) if !names.contains(key) =>
              key
          }
          if (typos.isEmpty) underlying.read(conf)
          else ConfError.invalidFields(typos, ev.settings.map(_.name)).notOk
        case els =>
          ConfError.typeMismatch("Object", els).notOk
      }
    }

}
