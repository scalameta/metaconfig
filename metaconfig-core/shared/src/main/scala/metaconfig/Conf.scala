package metaconfig

// This structure is like JSON except it doesn't support null.
sealed abstract class Conf extends Product with Serializable {
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
      vs.map { case (a, b) => s"$a: $b" }.mkString("{", ", ", "}")
  }
}
object Conf {
  case class Str(value: String) extends Conf
  case class Num(value: BigDecimal) extends Conf
  case class Bool(value: Boolean) extends Conf
  case class Lst(lst: List[Conf]) extends Conf
  object Lst { def apply(values: Conf*): Lst = Lst(values.toList) }
  case class Obj(values: List[(String, Conf)]) extends Conf {
    def keys: List[String] = values.map(_._1)
  }
  object Obj { def apply(values: (String, Conf)*): Obj = Obj(values.toList) }
}
