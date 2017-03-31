package metaconfig

import scala.language.implicitConversions

// Json-like ast.
sealed abstract class Conf extends Product with Serializable {
  import Conf._
  def simpleType: String = this match {
    case Str(_) => "String"
    case Num(_) => "Number"
    case Bool(_) => "Boolean"
    case Lst(_) => "List"
    case Obj(_) => "Map"
  }
  def simpleValue: String = this match {
    case Str(v) => v
    case Num(v) => v.toString()
    case Bool(v) => v.toString
    case Lst(vs) => vs.mkString("[", ", ", "]")
    case Obj(vs) =>
      vs.map { case (a, b) => s"$a: $b" }.mkString("{", ", ", "}")
  }
}
object Conf {
  case class Str(value: String) extends Conf
  object Str { implicit def string2Str(str: String): Conf = Str(str) }
  case class Num(value: BigDecimal) extends Conf
  object Num { implicit def int2num(int: Int): Conf = Num(int) }
  case class Bool(value: Boolean) extends Conf
  object Bool { implicit def boolean2Bool(b: Boolean): Conf = Bool(b) }
  case class Lst(lst: List[Conf]) extends Conf
  object Lst { def apply(values: Conf*): Lst = Lst(values.toList) }
  case class Obj(values: List[(String, Conf)]) extends Conf {
    def keys: List[String] = values.map(_._1)
  }
  object Obj { def apply(values: (String, Conf)*): Obj = Obj(values.toList) }
}
