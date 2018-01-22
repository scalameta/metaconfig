package metaconfig.docs

import scalatags.Text.all._
import metaconfig.generic.Setting
import metaconfig.generic.Settings

object Docs {
  def htmlSetting(setting: Setting, defaultValue: Any) = tr(
    th(setting.name),
    th(setting.field.tpe),
    th(setting.description),
    th(defaultValue.toString)
  )

  def html[T <: Product](default: T)(implicit settings: Settings[T]): String = {
    val fields = settings.flat(default).map {
      case (setting, defaultValue) =>
        htmlSetting(setting, defaultValue)
    }
    table(
      thead(
        tr(
          th("Name"),
          th("Type"),
          th("Description"),
          th("Default value")
        )
      ),
      tbody(fields)
    ).toString()
  }
}
