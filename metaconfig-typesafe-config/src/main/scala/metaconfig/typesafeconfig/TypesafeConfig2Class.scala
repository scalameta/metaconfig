package metaconfig
package typesafeconfig

import com.typesafe.config._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.meta.inputs._
import scala.meta.io.AbsolutePath

object TypesafeConfig2Class {
  def gimmeConfFromString(string: String): Configured[Conf] =
    gimmeSafeConf(() => ConfigFactory.parseString(string))
  def gimmeConfFromFile(file: java.io.File): Configured[Conf] = {
    if (!file.exists())
      Configured.NotOk(ConfError.fileDoesNotExist(file.getAbsolutePath))
    else if (file.isDirectory)
      Configured.NotOk(
        ConfError.msg(s"File ${file.getAbsolutePath} is a directory"))
    else gimmeSafeConf(() => ConfigFactory.parseFile(file))
  }
  def gimmeConf(config: Config): Configured[Conf] =
    gimmeSafeConf(() => config)

  private def gimmeSafeConf(config: () => Config): Configured[Conf] = {
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
            case null => Conf.Null()
            case x =>
              throw new IllegalArgumentException(
                s"Unexpected config value $value with unwrapped value $x")
          }
      }
      getPositionOpt(value.origin(), cache).fold(conf)(conf.withPos)
    }
    try {
      Configured.Ok(loop(config().resolve().root()))
    } catch {
      case e: ConfigException.Parse =>
        Configured.NotOk(
          ConfError.parseError(getPosition(e.origin(), cache), e.getMessage))
    }
  }

  private def getPosition(
      originOrNull: ConfigOrigin,
      cache: mutable.Map[Input, Array[Int]]): Position =
    getPositionOpt(originOrNull, cache).getOrElse(Position.None)

  private def getPositionOpt(
      originOrNull: ConfigOrigin,
      cache: mutable.Map[Input, Array[Int]]): Option[Position] =
    for {
      origin <- Option(originOrNull)
      url <- Option(origin.url())
      linePlus1 <- Option(origin.lineNumber())
      line = linePlus1 - 1
      input = Input.File(new java.io.File(url.toURI))
      offsetByLine = cache.getOrElseUpdate(
        input,
        Metaconfig.getOffsetByLine(input.chars))
      if line < offsetByLine.length
      start = offsetByLine(line)
    } yield Position.Range(input, start, start)

}
