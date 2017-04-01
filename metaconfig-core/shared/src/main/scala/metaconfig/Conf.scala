package metaconfig

import scala.util.Try

import metaconfig.Extractors._
import org.scalameta.logger

// This structure is like JSON except it doesn't support null.
sealed abstract class Conf extends Product with Serializable {
  def normalize: Conf = this
  import Conf._
  def kind: String = this match {
    case Str(_) => "String"
    case Num(_) => "Number"
    case Bool(_) => "Boolean"
    case Lst(_) => "List"
    case Obj(_) => "Map"
  }
  // TODO(olafur) use something like Paiges to get pretty output.
  def show: String = this match {
    case Str(v) => v
    case Num(v) => v.toString()
    case Bool(v) => v.toString
    case Lst(vs) => vs.mkString("[", ", ", "]")
    case Obj(vs) =>
      vs.map { case (a, b) => s"'$a': $b" }.mkString("{", ", ", "}")
  }

  override def toString: String = show
}
object Conf {
  case class Str(value: String) extends Conf {
    override def normalize: Conf = value match {
      case "true" | "on" | "yes" => Bool(true)
      case "false" | "off" | "no" => Bool(false)
      case Number(n) => Num(n)
      case _ => this
    }
  }
  case class Num(value: BigDecimal) extends Conf
  case class Bool(value: Boolean) extends Conf
  case class Lst(values: List[Conf]) extends Conf {
    override def normalize: Conf = Lst(values.map(_.normalize))
  }
  object Lst { def apply(values: Conf*): Lst = Lst(values.toList) }
  case class Obj(values: List[(String, Conf)]) extends Conf {
    def keys: List[String] = values.map(_._1)
    override def normalize: Conf =
      Obj(values.map {
        case (NestedKey(key, rest), value) =>
          logger.elem(logger.revealWhitespace(key))
          key -> Obj(rest -> value).normalize
        case (key, value) =>
          key -> value.normalize
      })
  }
  object Obj {
    def apply(values: (String, Conf)*): Obj = Obj(values.toList)
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
