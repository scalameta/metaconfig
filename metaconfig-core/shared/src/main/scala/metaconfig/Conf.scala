package metaconfig

import scala.util.Try

import metaconfig.Extractors._
import org.scalameta.logger

// This structure is like JSON except it doesn't support null.
sealed abstract class Conf extends Product with Serializable {
  def normalize: Conf = ConfOps.normalize(this)
  def kind: String = ConfOps.kind(this)
  def show: String = ConfOps.show(this)
  override def toString: String = show
}
object Conf {
  case class Str(value: String) extends Conf
  case class Num(value: BigDecimal) extends Conf
  case class Bool(value: Boolean) extends Conf
  case class Lst(values: List[Conf]) extends Conf {
    override def normalize: Conf = Lst(values.map(_.normalize))
  }
  object Lst { def apply(values: Conf*): Lst = Lst(values.toList) }
  case class Obj(values: List[(String, Conf)]) extends Conf {
    def keys: List[String] = values.map(_._1)
  }
  object Obj {
    def apply(values: (String, Conf)*): Obj = Obj(values.toList)
  }
}
object ConfOps {
  import Conf._
  // TODO(olafur) use something like Paiges to get pretty output.
  def show(conf: Conf): String = conf match {
    case Str(v) => v
    case Num(v) => v.toString()
    case Bool(v) => v.toString
    case Lst(vs) => vs.mkString("[", ", ", "]")
    case Obj(vs) =>
      vs.map { case (a, b) => s"'$a': $b" }.mkString("{", ", ", "}")
  }

  def normalize(conf: Conf): Conf = conf match {
    case Conf.Num(_) => conf
    case Conf.Bool(_) => conf
    case Conf.Str(str) =>
      str match {
        case "true" | "on" | "yes" => Bool(true)
        case "false" | "off" | "no" => Bool(false)
        case Number(n) => Num(n)
        case _ => conf
      }
    case Conf.Lst(values) => Conf.Lst(values.map(normalize))
    case Conf.Obj(values) =>
      Conf.Obj(
        values.map {
          case (NestedKey(key, rest), value) =>
            key -> normalize(Obj(rest -> value))
          case (key, value) =>
            key -> normalize(value)
        }
      )
  }
  def kind(conf: Conf): String = conf match {
    case Str(_) => "String"
    case Num(_) => "Number"
    case Bool(_) => "Boolean"
    case Lst(_) => "List"
    case Obj(_) => "Map"
  }
}

object Extractors {
  object Number {
    def unapply(arg: String): Option[BigDecimal] =
      Try(BigDecimal(arg)).toOption
  }
  object NestedKey {
    def unapply(arg: String): Option[(String, String)] = {
      val idx = arg.indexOf('.')
      if (idx == -1) None
      else {
        arg.splitAt(idx) match {
          case (_, "") => None
          case (a, b) => Some(a -> b.stripPrefix("."))
        }
      }
    }
  }
}
