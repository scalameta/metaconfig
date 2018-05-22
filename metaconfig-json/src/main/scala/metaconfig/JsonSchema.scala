package metaconfig

import metaconfig.generic.Setting
import metaconfig.generic.Settings
import ujson._

object JsonSchema {

  def generate[T: ConfEncoder](
      title: String,
      description: String,
      url: Option[String],
      default: T)(implicit settings: Settings[T]): Js.Obj = {
    ConfEncoder[T].write(default) match {
      case obj: Conf.Obj =>
        this.generate[T](title, description, url, obj)
      case els =>
        throw new IllegalArgumentException(s"Expected Conf.Obj, got $els")
    }
  }

  def generate[T](
      title: String,
      description: String,
      url: Option[String],
      default: Conf.Obj)(implicit settings: Settings[T]): Js.Obj = {

    val properties: List[(String, Js.Obj)] = settings.settings
      .zip(default.values)
      .map { case (s, (_, v)) => fromSetting(s, v) }
    pprint.log(properties)

    Js.Obj(
      "$id" -> url.map(Js.Str).getOrElse(Js.Null),
      "title" -> Js.Str(title),
      "description" -> Js.Str(description),
      "type" -> "object",
      "properties" -> Js.Obj(properties: _*)
    )
  }

  private def fromSetting(s: Setting, dv: Conf) = dv match {
    case p: Conf.Obj => fromComplexSetting(s, p)
    case v => fromSimpleSetting(s, v)
  }

  private def fromSimpleSetting(
      setting: Setting,
      defaultValue: Conf): (String, Js.Obj) = {

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
      defaultValue: Conf.Obj): (String, Js.Obj) = {
    val properties =
      setting.underlying
        .map(
          _.settings
            .zip(defaultValue.values)
            .map { case (s, (_, v)) => fromSetting(s, v) }
        )
        .getOrElse(Nil)

    pprint.log(properties)

    setting.name -> Js.Obj(
      "title" -> Js.Str(setting.name),
      "description" -> setting.description.map(Js.Str).getOrElse(Js.Null),
      "default" -> toJsonValue(defaultValue),
      "required" -> Js.False, // TODO: How should we handle required
      "type" -> "object",
      "properties" -> Js.Obj(properties: _*)
    )
  }

  private def toJsonValue(value: Conf): Js.Value = value match {
    case Conf.Obj(values) =>
      Js.Obj(values.map {
        case (k, v) => k -> toJsonValue(v)
      }: _*)
    case Conf.Lst(values) =>
      Js.Arr(values.map(toJsonValue))
    case Conf.Null() =>
      Js.Null
    case Conf.Str(value) =>
      Js.Str(value)
    case Conf.Num(value) =>
      Js.Num(value.toDouble)
    case Conf.Bool(value) =>
      Js.Bool(value)
  }

  private def toSchemaType(tpe: String): Js.Str = tpe match {
    // https://tools.ietf.org/html/draft-handrews-json-schema-01#section-4.2.1
    case "Boolean" => "boolean"
    case "Int" => "number"
    case "Float" => "number"
    case "List" => "array"
    case "String" => "string"
    case _ => "object"
  }

}
