package metaconfig.hocon

import scala.meta.inputs._

import fastparse.core.Parsed
import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Metaconfig

object Hocon2Class {
  def gimmeConfig(input: Input): metaconfig.Configured[Conf] = {
    HoconParser.root.parse(new String(input.chars)) match {
      case Parsed.Success(value, _) => Configured.Ok(value.normalize)
      case e @ Parsed.Failure(_, start, _) =>
        val pos = Position.Range(input, start, start)
        ConfError.parseError(pos, e.msg).notOk
    }
  }

  def gimmeClass[T](
      configStr: Input,
      reader: metaconfig.ConfDecoder[T],
      path: Option[String] = None): metaconfig.Configured[T] = {
    for {
      config <- gimmeConfig(configStr)
      clz <- reader.read(config.normalize)
    } yield clz
  }

}
