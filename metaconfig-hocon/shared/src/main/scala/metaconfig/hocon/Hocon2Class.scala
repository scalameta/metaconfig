package metaconfig.hocon

import scala.util.Try
import scala.util.control.NonFatal

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Hocon2Class {
  // Necessary until https://github.com/unicredit/shocon/pull/13 is merged
  private def unwrapStringAsNumber(value: String): Try[Any] =
    Try(value.toInt)
      .recover { case _ => value.toLong }
      .recover { case _ => value.toDouble }
  private def config2map(config: Config): Map[String, Any] = {
    import scala.collection.JavaConverters._
    def loop(obj: Any): Any = obj match {
      case map: java.util.Map[_, _] =>
        map.asScala.map {
          case (key, value) => key -> loop(value)
        }.toMap
      case map: java.util.List[_] =>
        map.asScala.map(loop).toList
      case e: String => unwrapStringAsNumber(e).getOrElse(e)
      case e => e
    }
    loop(config.root().unwrapped).asInstanceOf[Map[String, Any]]
  }

  def gimmeConfig(
      str: String,
      path: Option[String] = None): metaconfig.Result[Map[String, Any]] = {
    try {
      val config = ConfigFactory.parseString(str)
      val extracted = path match {
        case Some(p) =>
          config.getConfig(p)
        case _ => config
      }
      val result = config2map(extracted)
      org.scalameta.logger.elem(result)
      Right(result)
    } catch {
      case NonFatal(e) => Left(e)
    }
  }

  def gimmeClass[T](configStr: String,
                    reader: metaconfig.Reader[T],
                    path: Option[String] = None): metaconfig.Result[T] = {
    for {
      config <- gimmeConfig(configStr, path).right
      clz <- reader.read(config).right
    } yield clz
  }

}
