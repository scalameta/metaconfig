package metaconfig

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import metaconfig.internal.Levenshtein
import metaconfig.ConfError.TypeMismatch
import metaconfig.annotation.DeprecatedName
import metaconfig.error.CompositeException

// TODO(olafur) I think ConfError needs to be rethinked from scratch.
// It would be much cleaner as NonEmptyList[Error] where Error
// is an ADT with Exception | TypeMismatch | MissingFile | ...
sealed abstract class ConfError(val msg: String) extends Serializable { self =>
  def extra: List[String] = Nil
  final def all: List[String] = msg :: extra
  final override def toString: String =
    if (isEmpty) "No error message provided"
    else if (extra.isEmpty) stackTrace
    else {
      val sb = new StringWriter()
      val out = new PrintWriter(sb)
      if (extra.nonEmpty) out.println(s"${extra.length + 1} errors")
      all.zipWithIndex.foreach { case (err, i) =>
        out.append(s"[E$i] ").println(err)
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

  final def notOk: Configured[Nothing] = Configured.NotOk(this)
  final def left[A]: Either[ConfError, A] = Left(this)

  final override def hashCode(): Int =
    (msg.hashCode << 1) | (if (hasPos) 1 else 0)
  final override def equals(obj: scala.Any): Boolean = obj match {
    case err: ConfError => hasPos == err.hasPos && msg == err.msg
    case _ => false
  }

  def isEmpty: Boolean = msg.isEmpty && extra.isEmpty

  final def combine(other: ConfError): ConfError =
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
      new ConfError(position.pretty("error", msg)) {
        override def hasPos: Boolean = true
      }
    }

  // TODO(olafur) this is nothing but pure abusal of overriding and custom classes
  // Maybe it's better to model everything as a `List[ErrorADT]`
  def cause: Option[Throwable] = None
  final def isException: Boolean = cause.nonEmpty
  def isMissingField: Boolean = false
  def isParseError: Boolean = false
  def typeMismatch: Option[TypeMismatch] = None
  def isTypeMismatch: Boolean = false
  def isDeprecation: Boolean = false

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

  def deprecated(
      name: String,
      message: String,
      sinceVersion: String
  ): ConfError =
    deprecated(DeprecatedName(name, message, sinceVersion))
  def deprecated(deprecation: DeprecatedName): ConfError =
    new ConfError(deprecation.toString) {
      override def isDeprecation: Boolean = true
    }
  def message(message: String): ConfError =
    new ConfError(message) {}
  def exception(e: Throwable, stackSize: Int = 10): ConfError = {
    e.setStackTrace(e.getStackTrace.take(stackSize))
    new ConfError(e.getMessage) {
      override def cause: Option[Throwable] = Some(e)
    }
  }
  def fileDoesNotExist(path: Path): ConfError =
    fileDoesNotExist(path.toAbsolutePath.toString)
  def fileDoesNotExist(file: File): ConfError =
    fileDoesNotExist(file.getAbsolutePath)
  def fileDoesNotExist(path: String): ConfError =
    message(s"File $path does not exist.")
  def parseError(position: Position, message: String): ConfError =
    new ConfError(position.pretty("error", message)) {
      override def isParseError: Boolean = true
    }
  def typeMismatch(expected: String, obtained: Conf): ConfError =
    typeMismatch(expected, obtained, "")
  def typeMismatch(
      expected: String,
      obtained: Conf,
      path: String
  ): ConfError = {
    typeMismatch(expected, s"${obtained.kind} (value: $obtained)", path)
      .atPos(obtained.pos)
  }
  def typeMismatch(
      expected: String,
      obtained: String,
      path: String
  ): ConfError = {
    val pathSuffix = if (path.isEmpty) "" else s" at '$path'"
    new ConfError(
      s"""Type mismatch$pathSuffix;
        |  found    : $obtained
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
          else obj.keys.sorted.minBy(Levenshtein.distance(field))
        s" Did you mean '$closestField' instead?"
      }
    new ConfError(s"$obj has no field '$field'." + hint) {
      override def isMissingField: Boolean = true
    }
  }

  def invalidFields(
      invalid: Iterable[String],
      valid: Iterable[String]
  ): ConfError =
    invalidFieldsOpt(invalid, valid).getOrElse(empty)

  def invalidFieldsOpt(
      invalid: Iterable[String],
      valid: Iterable[String]
  ): Option[ConfError] = {
    invalidFieldsOpt(invalid.map(i => i -> Position.None), valid)
  }

  def invalidFields(
      invalid: Iterable[(String, Position)],
      valid: Iterable[String]
  )(implicit dummy: DummyImplicit): ConfError =
    invalidFieldsOpt(invalid, valid).getOrElse(empty)

  def invalidFieldsOpt(
      invalid: Iterable[(String, Position)],
      valid: Iterable[String]
  )(implicit dummy: DummyImplicit): Option[ConfError] = {
    val candidates = valid.toSeq
    val errors = invalid.toList.map { case (field, pos) =>
      val closestCandidate = Levenshtein.closestCandidate(field, candidates)
      val didYouMean = closestCandidate match {
        case None =>
          ""
        case Some(candidate) =>
          s"\n\tDid you mean '$candidate'?"
      }
      message(
        s"found option '$field' which wasn't expected, or isn't valid in this context.$didYouMean"
      ).atPos(pos)
    }
    apply(errors)
  }

  def fromResults(results: Seq[Configured[_]]): Option[ConfError] =
    apply(results.collect { case Configured.NotOk(x) => x })

  def apply(errors: Seq[ConfError]): Option[ConfError] = {
    def seqToOpt[T](values: Seq[T])(f: (T, Seq[T]) => T): Option[T] =
      values.headOption.map { head =>
        val tail = values.tail
        if (tail.isEmpty) head else f(head, tail)
      }
    seqToOpt(errors) { case (head, tail) =>
      new ConfError(head.stackTrace) {
        override def extra: List[String] =
          head.extra ++ tail.flatMap(x => x.stackTrace :: x.extra)
        override def typeMismatch: Option[TypeMismatch] =
          errors.view.flatMap(_.typeMismatch).headOption
        override def isParseError: Boolean = errors.exists(_.isParseError)
        override def isMissingField: Boolean = errors.exists(_.isMissingField)

        override def cause: Option[Throwable] =
          seqToOpt(errors.flatMap {
            _.cause match {
              case Some(c: CompositeException) => c.all
              case x => x
            }
          }) { case (head, tail) => CompositeException(head, tail.toList) }
      }
    }
  }

}
