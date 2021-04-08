package metaconfig.internal

import metaconfig.Conf
import metaconfig.Input

object JsonConverter {

  def fromInput(input: Input): ujson.Value = {
    val readable = ujson.Readable.fromTransformer(input, JsonConfParser)
    val js = readable.transform(ujson.Value)
    js
  }

  def toConf(js: ujson.Value): Conf = js match {
    case ujson.Obj(values) =>
      Conf.Obj(values.iterator.map {
        case (key, value) => key -> toConf(value)
      }.toList)
    case ujson.Arr(values) =>
      Conf.Lst(values.iterator.map(toConf).toList)
    case ujson.Bool(value) =>
      Conf.Bool(value)
    case ujson.Num(value) =>
      Conf.Num(value)
    case ujson.Str(value) =>
      Conf.Str(value)
    case ujson.Null =>
      Conf.Null()
  }

  import ujson.Obj._
  def toJson(conf: Conf): ujson.Value = conf match {
    case Conf.Obj(values) =>
      values.map { case (k, v) => k -> toJson(v) }
    case Conf.Lst(values) =>
      ujson.Arr(values.map(toJson): _*)
    case Conf.Null() =>
      ujson.Null
    case Conf.Str(value) =>
      ujson.Str(value)
    case Conf.Num(value) =>
      ujson.Num(value.toDouble)
    case Conf.Bool(value) =>
      ujson.Bool(value)
  }
}
