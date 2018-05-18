package metaconfig.schema

import metaconfig.generic.Setting
import metaconfig.generic.Settings
import ujson._

object Schema {

  def schema[T <: Product](
      title: String,
      description: String,
      url: Option[String],
      default: T)(implicit settings: Settings[T]): Js.Obj = {

    val properties: List[(String, Js.Obj)] = settings.settings.map(fromSetting)
    Js.Obj(
      "$id" -> url.map(Js.Str).getOrElse(Js.Null),
      "title" -> Js.Str(title),
      "description" -> Js.Str(description),
      "type" -> "object",
      "properties" -> Js.Obj(properties: _*)
    )
  }

  private def fromSetting(setting: Setting): (String, Js.Obj) = {
    val properties =
      setting.underlying.map(_.settings.map(fromSetting)).getOrElse(Nil)

    setting.name -> Js.Obj(
      "title" -> Js.Str(setting.name),
      "description" -> setting.description.map(Js.Str).getOrElse(Js.Null),
      "default" -> Js.Null, // TODO
      // TODO: How should we handle required
      "required" -> Js.False,
      "type" -> toSchemaType(setting.tpe),
      "properties" -> Js.Obj(properties: _*)
    )
  }

  private def toSchemaType(tpe: String): Js.Str = tpe match {
    case "Boolean" => "boolean"
    case "Int" => "int"
    case "Float" => "int"
    case "List" => "array"
    case "String" => "string"
    case _ => "object"
  }

}
