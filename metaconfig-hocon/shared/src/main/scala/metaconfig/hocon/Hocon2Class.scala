package metaconfig.hocon

import scala.meta.inputs._
import scala.util.control.NonFatal

import fastparse.core.Parsed
import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Metaconfig
import org.scalameta.logger

object Hocon2Class {
  def gimmeConfig(str: String): metaconfig.Configured[Conf] = {
    HoconParser.root.parse(str) match {
      case Parsed.Success(value, _) => Configured.Ok(value)
      case e @ Parsed.Failure(_, idx, _) =>
        val input = Input.String(str)
        val start = Point.Offset(input, idx)
        val pos = Position.Range(input, start, start)
        ConfError.parseError(pos, e.msg).notOk
    }
  }

  def gimmeClass[T](configStr: String,
                    reader: metaconfig.ConfDecoder[T],
                    path: Option[String] = None): metaconfig.Configured[T] = {
    for {
      config <- gimmeConfig(configStr)
      clz <- reader.read(config.normalize)
    } yield clz
  }

}
