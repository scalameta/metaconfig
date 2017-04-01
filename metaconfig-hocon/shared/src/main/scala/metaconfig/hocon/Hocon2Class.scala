package metaconfig.hocon

import scala.util.control.NonFatal

import fastparse.core.Parsed
import metaconfig.Conf
import org.scalameta.logger

object Hocon2Class {
  def gimmeConfig(str: String): metaconfig.Result[Conf] = {
    try {
      HoconParser.root.parse(str) match {
        case Parsed.Success(value, _) =>
          logger.elem(value.normalize)
          Right(value.normalize)
        case e @ Parsed.Failure(_, _, _) => Left(metaconfig.ParseError(e.msg))
      }
    } catch {
      case NonFatal(e) => Left(e)
    }
  }

  def gimmeClass[T](configStr: String,
                    reader: metaconfig.ConfDecoder[T],
                    path: Option[String] = None): metaconfig.Result[T] = {
    for {
      config <- gimmeConfig(configStr).right
      clz <- reader.read(config).right
    } yield clz
  }

}
