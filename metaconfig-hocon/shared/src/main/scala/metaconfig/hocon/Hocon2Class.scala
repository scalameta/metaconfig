package metaconfig.hocon

import scala.util.Try
import scala.util.control.NonFatal

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import eu.unicredit.{shocon => s}
import metaconfig.Conf

object Hocon2Class {
  private def unreachable() =
    throw new IllegalStateException("Null is not supported!")
  private def config2map(config: Config): Conf.Obj = {
    import scala.collection.JavaConverters._
    def loop(obj: s.Config.Value): Conf = obj match {
      case s.Config.Object(lst) =>
        Conf.Obj(
          lst
            .withFilter(_ != s.Config.NullLiteral)
            .map {
              case (a, b) =>
                a -> loop(b)
            }
            .toList
        )
      case s.Config.Array(lst) =>
        Conf.Lst(lst.withFilter(_ != s.Config.NullLiteral).map(loop).toList)
      case s.Config.StringLiteral(e) =>
        e match {
          case "true" | "on" | "yes" => Conf.Bool(true)
          case "false" | "off" | "no" => Conf.Bool(false)
          case _ => Try(Conf.Num(BigDecimal(e))).getOrElse(Conf.Str(e))
        }
      case s.Config.NumberLiteral(num) => Conf.Num(BigDecimal(num))
      case s.Config.BooleanLiteral(b) => Conf.Bool(b)
      case s.Config.NullLiteral => unreachable()
      case _: s.Config.SimpleValue => unreachable()
    }
    val root = config.root()
    val values = root.asScala.map {
      case (a, b) => a -> loop(b.inner)
    }
    Conf.Obj(values.toList)
  }

  def gimmeConfig(str: String,
                  path: Option[String] = None): metaconfig.Result[Conf] = {
    try {
      val config = ConfigFactory.parseString(str)
      val extracted = path match {
        case Some(p) =>
          config.getConfig(p)
        case _ => config
      }
      val result = config2map(extracted)
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
