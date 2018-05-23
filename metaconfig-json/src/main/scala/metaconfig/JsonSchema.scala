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

    Js.Obj(
      "$id" -> url.map(Js.Str).getOrElse(Js.Null),
      "title" -> Js.Str(title),
      "description" -> Js.Str(description),
      "type" -> "object",
      "properties" -> Js.Obj(properties: _*)
    )
  }

  private def fromSetting(
      setting: Setting,
      defaultValue: Conf
  ): (String, Js.Obj) = {
    val defaultJsonValue = toJsonValue(defaultValue)
    val obj = Js.Obj(
      "title" -> Js.Str(setting.name),
      "description" -> setting.description.map(Js.Str).getOrElse(Js.Null),
      "default" -> defaultJsonValue,
      "required" -> Js.False, // TODO: How should we handle required
      "type" -> toSchemaType(defaultValue)
    )
    defaultValue match {
      case Conf.Obj(values) =>
        val properties = setting.underlying
          .map(
            _.settings
              .zip(values)
              .map { case (s, (_, v)) => fromSetting(s, v) }
          )
          .getOrElse(Nil)
        obj.obj.put("properties", properties)
      case _ =>
    }

    setting.name -> obj
  }

  private def toJsonValue(conf: Conf): Js.Value = conf match {
    case Conf.Obj(values) =>
      Js.Obj(values.map {
        case (k, v) => k -> toJsonValue(v)
      }: _*)
    case Conf.Lst(values) =>
      Js.Arr(values.map(toJsonValue): _*)
    case Conf.Null() =>
      Js.Null
    case Conf.Str(value) =>
      Js.Str(value)
    case Conf.Num(value) =>
      Js.Num(value.toDouble)
    case Conf.Bool(value) =>
      Js.Bool(value)
  }

  private def toSchemaType(conf: Conf): Js.Str = Js.Str(
    conf match {
      // https://tools.ietf.org/html/draft-handrews-json-schema-01#section-4.2.1
      case _: Conf.Bool => "boolean"
      case _: Conf.Num => "number"
      case _: Conf.Lst => "array"
      case _: Conf.Str => "string"
      case _ => "object"
    }
  )

}
