package mopt
package typesafeconfig

import com.typesafe.config._
import scala.collection.JavaConverters._
import scala.collection.mutable

object TypesafeConfig2Class {
  def gimmeConfFromString(string: String): Configured[Conf] =
    gimmeSafeConf(Input.String(string), () => ConfigFactory.parseString(string))
  def gimmeConfFromStringFilename(
      filename: String,
      string: String
  ): Configured[Conf] =
    gimmeSafeConf(
      Input.VirtualFile(filename, string),
      () => ConfigFactory.parseString(string)
    )
  def gimmeConfFromFile(file: java.io.File): Configured[Conf] = {
    if (!file.exists())
      Configured.NotOk(ConfError.fileDoesNotExist(file.getAbsolutePath))
    else if (file.isDirectory)
      Configured.NotOk(
        ConfError.message(s"File ${file.getAbsolutePath} is a directory")
      )
    else gimmeSafeConf(Input.File(file), () => ConfigFactory.parseFile(file))
  }
  def gimmeConf(config: Config): Configured[Conf] =
    gimmeSafeConf(Input.None, () => config)

  private def gimmeSafeConf(
      input: Input,
      config: () => Config
  ): Configured[Conf] = {
    val cache = mutable.Map.empty[Input, Array[Int]]
    def loop(value: ConfigValue): Conf = {
      val conf = value match {
        case obj: ConfigObject =>
          Conf.Obj(obj.asScala.mapValues(loop).toList)
        case lst: ConfigList =>
          Conf.Lst(lst.listIterator().asScala.map(loop).toList)
        case _ =>
          value.unwrapped match {
            case x: String => Conf.Str(x)
            case x: java.lang.Integer => Conf.Num(BigDecimal(x))
            case x: java.lang.Long => Conf.Num(BigDecimal(x))
            case x: java.lang.Double => Conf.Num(BigDecimal(x))
            case x: java.lang.Boolean => Conf.Bool(x)
            case null => Conf.Null()
            case x =>
              throw new IllegalArgumentException(
                s"Unexpected config value $value with unwrapped value $x"
              )
          }
      }
      getPositionOpt(value.origin, input) match {
        case Some(pos) => conf.withPos(pos)
        case None => conf
      }
    }
    try {
      Configured.Ok(loop(config().resolve().root))
    } catch {
      case e: ConfigException.Parse =>
        Configured.NotOk(
          ConfError.parseError(getPosition(e.origin, input), e.getMessage)
        )
    }
  }

  private def getPosition(
      originOrNull: ConfigOrigin,
      input: Input
  ): Position =
    getPositionOpt(originOrNull, input).getOrElse(Position.None)

  private def getPositionOpt(
      originOrNull: ConfigOrigin,
      input: Input
  ): Option[Position] =
    for {
      origin <- Option(originOrNull)
      linePlus1 <- Option(origin.lineNumber)
      line = linePlus1 - 1
      start = input.lineToOffset(line)
    } yield Position.Range(input, start, start)

}
