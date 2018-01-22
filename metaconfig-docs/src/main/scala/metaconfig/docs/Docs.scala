package metaconfig.docs

import scalatags.Text.all._
import metaconfig.generic.Setting
import metaconfig.generic.Settings

object Docs {
  def htmlSetting(setting: Setting, defaultValue: Any) = tr(
    td(code(setting.name)),
    td(code(setting.field.tpe)),
    td(setting.description),
    td(defaultValue.toString)
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
