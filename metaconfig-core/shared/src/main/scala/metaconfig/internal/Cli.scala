package metaconfig.internal

import metaconfig.generic.Settings
import org.typelevel.paiges.Doc._

object Cli {
  def help[T](
      default: T)(implicit settings: Settings[T], ev: T <:< Product): String = {
    val keyValues = settings.withDefault(default).map {
      case (setting, value) =>
        val name = Case.camelToKebab(setting.name)
        val key = s"--$name: ${setting.tpe} = $value"
        key -> paragraph(setting.description.getOrElse(""))
    }
    tabulate(keyValues).render(80)
  }

}
