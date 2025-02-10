package metaconfig
package sconfig

import metaconfig.internal.ConfGet

import scala.collection.mutable
import scala.jdk.CollectionConverters._

import org.ekrich.config._

object SConfig2Class {
  def gimmeConfFromString(string: String): Configured[Conf] =
    gimmeSafeConf(ConfigFactory.parseString(string), Some(Input.String(string)))

  /** Does not work under Scala.JS */
  def gimmeConfFromFile(file: java.io.File): Configured[Conf] =
    if (!file.exists()) Configured
      .NotOk(ConfError.fileDoesNotExist(file.getAbsolutePath))
    else if (file.isDirectory) Configured
      .NotOk(ConfError.message(s"File ${file.getAbsolutePath} is a directory"))
    else gimmeSafeConf(ConfigFactory.parseFile(file))

  def gimmeConf(config: Config): Configured[Conf] = gimmeSafeConf(config)

  private[sconfig] def gimmeSafeConf(
      config: => Config,
      input: Option[Input] = None,
  ): Configured[Conf] = {
    val cache = mutable.Map.empty[Input, Array[Int]]
    def loop(value: ConfigValue): Conf = {
      val conf = value match {
        case obj: ConfigObject => Conf
            .Obj(obj.asScala.toList.map { case (k, v) => k -> loop(v) })
        case lst: ConfigList => Conf
            .Lst(lst.listIterator().asScala.map(loop).toList)
        case _ => value.unwrapped match {
            case x: String => Conf.Str(x)
            case x: java.lang.Integer => Conf.Num(BigDecimal(x))
            case x: java.lang.Long => Conf.Num(BigDecimal(x))
            case x: java.lang.Double => Conf.Num(BigDecimal(x))
            case x: java.lang.Boolean => Conf.Bool(x)
            case null => Conf.Null()
            case x => throw new IllegalArgumentException(
                s"Unexpected config value $value with unwrapped value $x",
              )
          }
      }
      getPosition(value.origin, cache, input).fold(conf)(conf.withPos)
    }
    try Configured.Ok(loop(config.resolve().root))
    catch {
      case e: ConfigException.Parse =>
        val pos = getPosition(e.origin, cache, input).getOrElse(Position.None)
        Configured.NotOk(ConfError.parseError(pos, e.getMessage))
    }
  }

  private def getPosition(
      originOrNull: ConfigOrigin,
      cache: mutable.Map[Input, Array[Int]],
      input: Option[Input] = None,
  ): Option[Position] = for {
    origin <- Option(originOrNull)
    line = origin.lineNumber - 1 if line >= 0
    input <- Option(origin.filename).flatMap { f =>
      if (input.exists(_.path == f)) None
      else Some(Input.VirtualFile(f, PlatformInput.readFile(f, "utf-8")))
    }.orElse(Option(origin.url).flatMap(PlatformFileOps.fromURL)).orElse(input)
    offsetByLine = cache
      .getOrElseUpdate(input, ConfGet.getOffsetByLine(input.chars))
    if line < offsetByLine.length
    start = offsetByLine(line)
  } yield Position.Range(input, start, start)

}
