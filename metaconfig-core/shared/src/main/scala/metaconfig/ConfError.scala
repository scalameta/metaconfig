package metaconfig

import scala.meta.internal.inputs._
import scala.meta.inputs.Position
import scala.compat.Platform.EOL

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
  def fileDoesNotExist(path: String): ConfError =
    new ConfError(s"File $path does not exist.") {}
  def parseError(position: Position, msg: String): ConfError =
    new ConfError(position.formatMessage("parseerror", msg)) {}

  def typeMismatch(expected: String, obtained: Conf): ConfError =
    new ConfError(
      s"""Type mismatch;
         |  found    : ${obtained.kind} (value: $obtained)
         |  expected : $expected""".stripMargin
    ) {}

  // TOOD(olafur) levenshtein
  def invalidFields(invalid: Iterable[String],
                    valid: Iterable[String]): ConfError =
    new ConfError(s"Invalid fields: ${invalid.mkString(", ")}") {}

  def fromResults(results: Seq[Configured[_]]): Option[ConfError] =
    apply(results.collect { case Configured.NotOk(x) => x })

  def apply(errors: Seq[ConfError]): Option[ConfError] = {
    if (errors.isEmpty) None
    else Some(errors.foldLeft(empty)(_ combine _))
  }
}
