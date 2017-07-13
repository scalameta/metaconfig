package metaconfig

import scala.meta.internal.inputs._
import scala.meta.inputs.Position
import scala.compat.Platform.EOL

// TODO(olafur) use richer "Message" data type instead of String to support
// - exceptions with full stack trace
// - positions
sealed abstract class ConfError(val msg: String) extends Serializable { self =>
  def extra: List[String] = Nil
  override def toString: String =
    if (isEmpty) "No error message"
    else if (extra.isEmpty) msg
    else {
      val all = msg :: extra
      val orderedList = all.mkString("\n\n")
      orderedList
    }
  def notOk = Configured.NotOk(this)
  override def hashCode(): Int = (msg.hashCode << 1) | (if (hasPos) 1 else 0)

  override def equals(obj: scala.Any): Boolean = obj match {
    case err: ConfError => hasPos == err.hasPos && msg == err.msg
    case _ => false
  }

  def isEmpty = msg.isEmpty && extra.isEmpty

  def hasPos: Boolean = false
  def combine(other: ConfError): ConfError =
    if (isEmpty) other
    else if (other.isEmpty) this
    else {
      new ConfError(msg) {
        override def extra: List[String] =
          other.msg :: (other.extra ++ self.extra)
      }
    }

  def atPos(position: Position): ConfError =
    if (hasPos) this // avoid duplicate position
    else {
      new ConfError(position.formatMessage("error", msg)) {
        override def hasPos: Boolean = true
      }
    }
}

object ConfError {
  lazy val empty = new ConfError("") {}

  def msg(message: String): ConfError =
    new ConfError(message) {}
  def exception(e: Throwable, stackSize: Int = 10): ConfError =
    msg(s"""$e
           |${e.getStackTrace.take(stackSize).mkString("\n")}""".stripMargin)
  def fileDoesNotExist(path: String): ConfError =
    msg(s"File $path does not exist.")
  def parseError(position: Position, message: String): ConfError =
    msg(position.formatMessage("parseerror", message))
  def typeMismatch(expected: String, obtained: Conf): ConfError =
    msg(
      s"""Type mismatch;
         |  found    : ${obtained.kind} (value: $obtained)
         |  expected : $expected""".stripMargin
    )
  def missingField(obj: Conf.Obj, field: String): ConfError = {
    val closestField =
      if (obj.values.isEmpty) ""
      else obj.keys.sorted.minBy(levenshtein(field))
    msg(
      s"Object $obj has no field '$field'. " +
        s"Did you mean '$closestField' instead?")
  }

  // TOOD(olafur) levenshtein
  def invalidFields(
      invalid: Iterable[String],
      valid: Iterable[String]): ConfError =
    new ConfError(s"Invalid fields: ${invalid.mkString(", ")}") {}

  def fromResults(results: Seq[Configured[_]]): Option[ConfError] =
    apply(results.collect { case Configured.NotOk(x) => x })

  def apply(errors: Seq[ConfError]): Option[ConfError] = {
    if (errors.isEmpty) None
    else Some(errors.foldLeft(empty)(_ combine _))
  }

  /** Levenshtein distance. Implementation based on Wikipedia's algorithm. */
  private def levenshtein(s1: String)(s2: String): Int = {
    val dist = Array.tabulate(s2.length + 1, s1.length + 1) { (j, i) =>
      if (j == 0) i else if (i == 0) j else 0
    }

    for (j <- 1 to s2.length; i <- 1 to s1.length)
      dist(j)(i) =
        if (s2(j - 1) == s1(i - 1))
          dist(j - 1)(i - 1)
        else
          dist(j - 1)(i)
            .min(dist(j)(i - 1))
            .min(dist(j - 1)(i - 1)) + 1

    dist(s2.length)(s1.length)
  }
}
