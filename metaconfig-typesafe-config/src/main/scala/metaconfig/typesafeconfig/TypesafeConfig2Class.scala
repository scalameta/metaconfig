package metaconfig
package typesafeconfig

import com.typesafe.config._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.meta.inputs._
import scala.meta.io.AbsolutePath

object TypesafeConfig2Class {
  def gimmeConfFromString(string: String): Result[Conf] =
    gimmeSafeConf(() => ConfigFactory.parseString(string))
  def gimmeConfFromFile(file: java.io.File): Result[Conf] =
    gimmeSafeConf(() => ConfigFactory.parseFile(file))
  def gimmeConf(config: Config): Result[Conf] =
    gimmeSafeConf(() => config)

  private def gimmeSafeConf(f: () => Config): Result[Conf] = {
    val cache = mutable.Map.empty[Input, Array[Int]]
    def loop(value: ConfigValue): Conf = {
      val conf = value match {
        case x: ConfigObject =>
          Conf.Obj(
            x.keySet().asScala.map(key => key -> loop(x.get(key))).toList)
        case x: ConfigList =>
          Conf.Lst(x.listIterator().asScala.map(loop).toList)
        case _ =>
          value.unwrapped() match {
            case x: String => Conf.Str(x)
            case x: java.lang.Integer => Conf.Num(BigDecimal(x))
            case x: java.lang.Long => Conf.Num(BigDecimal(x))
            case x: java.lang.Double => Conf.Num(BigDecimal(x))
            case x: java.lang.Boolean => Conf.Bool(x)
            case x =>
              throw new IllegalArgumentException(
                s"Unexpected config value $value with unwrapped value $x")
          }
      }
      getPosition(value, cache).fold(conf)(conf.withPos)
    }
    try {
      Right(loop(f().resolve().root()))
    } catch {
      case scala.util.control.NonFatal(e) =>
        Left(e)
    }
  }

  // Copy-pasted from scala.meta inputs because it's private.
  // TODO(olafur) expose utility in inputs to get offset from line
  private def getOffsetByLine(chars: Array[Char]): Array[Int] = {
    val buf = new mutable.ArrayBuffer[Int]
    buf += 0
    var i = 0
    while (i < chars.length) {
      if (chars(i) == '\n') buf += (i + 1)
      i += 1
    }
    if (buf.last != chars.length) buf += chars.length // sentinel value used for binary search
    buf.toArray
  }

  private def getPosition(
      value: ConfigValue,
      cache: mutable.Map[Input, Array[Int]]): Option[Position] =
    for {
      origin <- Option(value.origin())
      url <- Option(origin.url())
      line <- Option(origin.lineNumber())
      input = Input.File(new java.io.File(url.toURI))
      offsetByLine = cache.getOrElseUpdate(input, getOffsetByLine(input.chars))
      point = Point.Offset(input, offsetByLine(line))
    } yield Position.Range(input, point, point)

}
