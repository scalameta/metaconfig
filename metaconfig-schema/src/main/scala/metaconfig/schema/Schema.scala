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

    val properties: List[(String, Js.Obj)] = settings.settings
      .zip(default.productIterator.toIterable)
      .map { case (s, v) => fromSetting(s, v) }

    Js.Obj(
      "$id" -> url.map(Js.Str).getOrElse(Js.Null),
      "title" -> Js.Str(title),
      "description" -> Js.Str(description),
      "type" -> "object",
      "properties" -> Js.Obj(properties: _*)
    )
  }

  private def fromSetting(s: Setting, dv: Any) = dv match {
    case p: Product => fromComplexSetting(s, p)
    case v => fromSimpleSetting(s, v)
  }

  private def fromSimpleSetting(
      setting: Setting,
      defaultValue: Any): (String, Js.Obj) = {

    setting.name -> Js.Obj(
      "title" -> Js.Str(setting.name),
      "description" -> setting.description.map(Js.Str).getOrElse(Js.Null),
      "default" -> toJsonValue(defaultValue),
      "required" -> Js.False, // TODO: How should we handle required
      "type" -> toSchemaType(setting.tpe),
      "properties" -> Js.Obj()
    )
  }

  private def fromComplexSetting(
      setting: Setting,
      defaultValue: Product): (String, Js.Obj) = {
    val properties =
      setting.underlying
        .map(
          _.settings
            .zip(defaultValue.productIterator.toIterable)
            .map { case (s, v) => fromSetting(s, v) }
        )
        .getOrElse(Nil)

    setting.name -> Js.Obj(
      "title" -> Js.Str(setting.name),
      "description" -> setting.description.map(Js.Str).getOrElse(Js.Null),
      "default" -> Js.Null, // TODO
      "required" -> Js.False, // TODO: How should we handle required
      "type" -> "object",
      "properties" -> Js.Obj(properties: _*)
    )
  }

  private def toJsonValue(value: Any): Js.Value = value match {
    case i: Int => Js.Num(i)
    case s: String => Js.Str(s)
    case _ => Js.Null
  }

  private def toSchemaType(tpe: String): Js.Str = tpe match {
    // https://tools.ietf.org/html/draft-handrews-json-schema-01#section-4.2.1
    case "Boolean" => "boolean"
    case "Int" => "int"
    case "Float" => "int"
    case "List" => "array"
    case "String" => "string"
    case _ => "object"
  }

}
