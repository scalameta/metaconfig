package metaconfig
package typesafeconfig

import com.typesafe.config._
import scala.collection.mutable
import scala.jdk.CollectionConverters._

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
    val cache = mutable.Map.empty[OriginId, Input]
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
      getPositionOpt(value.origin, cache) match {
        case Some(pos) => conf.withPos(pos)
        case None => conf
      }
    }
    try {
      val resolved = config().resolve().root()
      cache += OriginId(resolved.origin) -> input
      Configured.Ok(loop(resolved))
    } catch {
      case e: ConfigException.Parse =>
        Configured.NotOk(
          ConfError.parseError(getPosition(e.origin, cache), e.getMessage)
        )
    }
  }

  private def getPosition(
      originOrNull: ConfigOrigin,
      cache: mutable.Map[OriginId, Input]
  ): Position =
    getPositionOpt(originOrNull, cache).getOrElse(Position.None)

  private def getPositionOpt(
      originOrNull: ConfigOrigin,
      cache: mutable.Map[OriginId, Input]
  ): Option[Position] =
    for {
      origin <- Option(originOrNull)
      input <- cache.updateWith(OriginId(origin)) { maybeCurrent =>
        maybeCurrent
          .orElse(
            Option(origin.url)
              .map(url => Input.File(new java.io.File(url.toURI)))
          )
      }
      linePlus1 = origin.lineNumber
      start = input.lineToOffset(linePlus1 - 1)
    } yield Position.Range(input, start, start)

  private case class OriginId private (id: String)
  private object OriginId {
    def apply(origin: ConfigOrigin): OriginId =
      OriginId(
        origin
          .withLineNumber(-1) // strip potential line suffix
          // use description instead of the nullable filename() or url(),
          // so that we can have an identifier to map to the main input
          // when using ConfigFactory.parseString(string)
          // https://github.com/lightbend/config/blob/f92a4ee/config/src/main/java/com/typesafe/config/impl/Parseable.java#L467
          .description()
      )
  }

  implicit private class Scala213MapBackport[K, V](map: mutable.Map[K, V]) {
    // https://github.com/scala/scala/blob/744651d/src/library/scala/collection/mutable/Map.scala#L106-L127
    def updateWith(
        key: K
    )(remappingFunction: Option[V] => Option[V]): Option[V] = {
      val previousValue = map.get(key)
      val nextValue = remappingFunction(previousValue)
      (previousValue, nextValue) match {
        case (None, None) => // do nothing
        case (Some(_), None) => map.remove(key)
        case (_, Some(v)) => map.update(key, v)
      }
      nextValue
    }
  }
}
