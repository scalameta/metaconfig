package metaconfig.docs

import scalatags.Text.all._
import metaconfig.annotation.Description
import metaconfig.generic
import metaconfig.generic.Settings

object Docs {
  case class User(
      @Description("Name description")
      name: String = "John",
      @Description("Age description")
      age: Int = 42
  )
  implicit val surface = generic.deriveSurface[User]
  def main(args: Array[String]): Unit = {
    println(html(User()))
  }
  def html[T <: Product](default: T)(implicit settings: Settings[T]): String = {
    val fields = settings.settings
      .zip(default.productIterator.toIterable)
      .map {
        case (setting, defaultSetting) =>
          tr(
            th(setting.name),
            th(setting.field.tpe),
            th(setting.description),
            th(defaultSetting.toString)
          )
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
