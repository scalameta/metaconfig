package metaconfig.internal

import metaconfig.Conf
import metaconfig.ConfEncoder
import metaconfig.ConfError
import metaconfig.annotation.Inline
import metaconfig.generic.Setting
import metaconfig.generic.Settings
import org.typelevel.paiges.Doc
import org.typelevel.paiges.Doc._

object Cli {
  def help[T: ConfEncoder](default: T)(implicit settings: Settings[T]): Doc = {
    def toHelp(setting: Setting, value: Conf) = {
      val name = Case.camelToKebab(setting.name)
      val key = s"--$name: ${setting.tpe} = $value "
      key -> paragraph(setting.description.getOrElse(""))
    }

    val defaultConf = ConfEncoder[T].write(default) match {
      case Conf.Obj(values) => values.map(_._2)
      case els => ConfError.typeMismatch("Conf.Obj", els).notOk.get
    }

    val keyValues = settings.settings.zip(defaultConf).flatMap {
      case (setting, value) =>
        if (setting.annotations.exists(_.isInstanceOf[Inline])) {
          for {
            underlying <- setting.underlying.toList
            (field, (_, fieldDefault)) <- underlying.settings.zip(
              value.asInstanceOf[Conf.Obj].values)
          } yield toHelp(field, fieldDefault)
        } else {
          toHelp(setting, value) :: Nil
        }
    }
    tabulate(keyValues)
  }
}
