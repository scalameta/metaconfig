package metaconfig

import metaconfig.generic.Setting
import metaconfig.generic.Settings
import metaconfig.internal.JsonConverter

object JsonSchema {

  def generate[T: ConfEncoder](
      title: String,
      description: String,
      url: Option[String],
      default: T
  )(implicit settings: Settings[T]): ujson.Value = {
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
      default: Conf.Obj
  )(implicit settings: Settings[T]): ujson.Value = {

    val properties: List[(String, ujson.Value)] = settings.settings
      .zip(default.values)
      .map { case (s, (_, v)) => fromSetting(s, v) }

    ujson.Obj(
      "$id" -> url.map(ujson.Str).getOrElse(ujson.Null),
      "title" -> ujson.Str(title),
      "description" -> ujson.Str(description),
      "type" -> "object",
      "properties" -> properties
    )
  }

  private def fromSetting(
      setting: Setting,
      defaultValue: Conf
  ): (String, ujson.Value) = {
    val defaultJsonValue = JsonConverter.toJson(defaultValue)
    val obj = ujson.Obj(
      "title" -> ujson.Str(setting.name),
      "description" -> setting.description.map(ujson.Str).getOrElse(ujson.Null),
      "default" -> defaultJsonValue,
      "required" -> ujson.False, // TODO: How should we handle required
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

  private def toSchemaType(conf: Conf): ujson.Str = ujson.Str(
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
