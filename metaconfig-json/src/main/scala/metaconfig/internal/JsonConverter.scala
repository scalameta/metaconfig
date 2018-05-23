package metaconfig.internal

import metaconfig.Conf
import metaconfig.Input
import ujson.Js
import ujson.Transformable

object JsonConverter {

  def fromInput(input: Input): Js = {
    val transformable =
      Transformable.fromTransformer(input, JsonConfParser)
    val js = transformable.transform(Js)
    js
  }

  def toConf(js: Js): Conf = js match {
    case Js.Obj(values) =>
      Conf.Obj(values.iterator.map {
        case (key, value) => key -> toConf(value)
      }.toList)
    case Js.Arr(values) =>
      Conf.Lst(values.iterator.map(toConf).toList)
    case Js.Bool(value) =>
      Conf.Bool(value)
    case Js.Num(value) =>
      Conf.Num(value)
    case Js.Str(value) =>
      Conf.Str(value)
    case Js.Null =>
      Conf.Null()
  }

  def toJson(conf: Conf): Js.Value = conf match {
    case Conf.Obj(values) =>
      Js.Obj(values.map {
        case (k, v) => k -> toJson(v)
      }: _*)
    case Conf.Lst(values) =>
      Js.Arr(values.map(toJson): _*)
    case Conf.Null() =>
      Js.Null
    case Conf.Str(value) =>
      Js.Str(value)
    case Conf.Num(value) =>
      Js.Num(value.toDouble)
    case Conf.Bool(value) =>
      Js.Bool(value)
  }
}
