package metaconfig

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import scala.meta.inputs.Position
import scala.meta.internal.inputs._
import metaconfig.ConfError.TypeMismatch

// TODO(olafur) use richer "Message" data type instead of String to support
// - exceptions with full stack trace
// - positions
sealed abstract class ConfError(val msg: String) extends Serializable { self =>
  def extra: List[String] = Nil
  final def all: List[String] = msg :: extra
  final override def toString: String =
    if (isEmpty) "No error message provided"
    else if (extra.isEmpty) msg
    else {
      val sb = new StringWriter()
      val out = new PrintWriter(sb)
      if (extra.nonEmpty) out.println(s"${extra.length + 1} errors")
      all.zipWithIndex.foreach {
        case (err, i) => out.append(s"[E$i] ").println(err)
      }
      sb.toString
    }
  final def stackTrace: String = cause match {
    case Some(ex) =>
      val baos = new ByteArrayOutputStream()
      ex.printStackTrace(new PrintStream(baos))
      baos.toString
    case None => msg
  }

  final def notOk = Configured.NotOk(this)
  final override def hashCode(): Int =
    (msg.hashCode << 1) | (if (hasPos) 1 else 0)
  final override def equals(obj: scala.Any): Boolean = obj match {
    case err: ConfError => hasPos == err.hasPos && msg == err.msg
    case _ => false
  }

  def isEmpty: Boolean = msg.isEmpty && extra.isEmpty

  def combine(other: ConfError): ConfError =
    if (isEmpty) other
    else if (other.isEmpty) this
    else {
      new ConfError(stackTrace) {
        override def extra: List[String] =
          other.stackTrace :: (other.extra ++ self.extra)
        override def cause: Option[Throwable] =
          if (self.cause.isEmpty) other.cause
          else if (other.cause.isEmpty) self.cause
          else Some(CompositeException(self.cause.get, other.cause.get))
        override def isParseError: Boolean =
          this.isParseError || other.isParseError
        override def isMissingField: Boolean =
          this.isMissingField || other.isMissingField
        override def typeMismatch: Option[TypeMismatch] =
          self.typeMismatch.orElse(other.typeMismatch)
      }
    }

  def hasPos: Boolean = false
  final def atPos(position: Position): ConfError =
    if (hasPos) this // avoid duplicate position
    else if (position == Position.None) this
    else {
      new ConfError(position.formatMessage("error", msg)) {
        override def hasPos: Boolean = true
      }
    }

  def cause: Option[Throwable] = None
  final def isException: Boolean = cause.nonEmpty
  def isMissingField: Boolean = false
  def isParseError: Boolean = false
  def typeMismatch: Option[TypeMismatch] = None
  def isTypeMismatch: Boolean = false

  def copy(newMsg: String): ConfError = new ConfError(newMsg) {
    override def hasPos: Boolean = self.hasPos
    override def cause: Option[Throwable] = self.cause
    override def isTypeMismatch: Boolean = self.isTypeMismatch
    override def isParseError: Boolean = self.isParseError
    override def isMissingField: Boolean = self.isMissingField
  }
}

object ConfError {

  case class TypeMismatch(obtained: String, expected: String, path: String)

  lazy val empty: ConfError = new ConfError("") {}

  def msg(message: String): ConfError =
    new ConfError(message) {}
  def exception(e: Throwable, stackSize: Int = 10): ConfError = {
    e.setStackTrace(e.getStackTrace.take(stackSize))
    new ConfError(e.getMessage) {
      override def cause: Option[Throwable] = Some(e)
    }
  }
  def fileDoesNotExist(path: String): ConfError =
    msg(s"File $path does not exist.")
  def parseError(position: Position, message: String): ConfError =
    new ConfError(position.formatMessage("error", message)) {
      override def isParseError: Boolean = true
    }
  def typeMismatch(expected: String, obtained: Conf): ConfError =
    typeMismatch(expected, obtained, "")
  def typeMismatch(expected: String, obtained: Conf, path: String): ConfError = {
    val pathSuffix = if (path.isEmpty) "" else s" at path '$path'"
    new ConfError(
      s"""Type mismatch$pathSuffix;
         |  found    : ${obtained.kind} (value: $obtained)
         |  expected : $expected""".stripMargin
    ) {
      override def isTypeMismatch: Boolean = true
    }
  }
  def missingField(obj: Conf.Obj, field: String): ConfError = {
    val hint =
      if (obj.values.lengthCompare(1) <= 0) ""
      else {
        val closestField =
          if (obj.values.isEmpty) ""
          else obj.keys.sorted.minBy(levenshtein(field))
        s" Did you mean '$closestField' instead?"
      }
    new ConfError(s"$obj has no field '$field'." + hint) {
      override def isMissingField: Boolean = true
    }
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
