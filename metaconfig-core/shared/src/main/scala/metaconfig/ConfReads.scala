package metaconfig

import java.util.NoSuchElementException

sealed abstract class Op
object Op {
  final case class DownField(field: String) extends Op
  final case class DownN(n: Int) extends Op
}

// TODO(olafur) builder
final case class Cursor(conf: Conf, history: List[Op])
object Cursor {
  def apply(conf: Conf): Cursor = Cursor(conf, Nil)
}

trait ConfReads[T] {
  def read(cursor: Cursor): ConfReads.Result[T]
}
object ConfReads {
  def read[T](conf: Conf)(implicit ev: ConfReads[T]): ConfReads.Result[T] =
    ev.read(Cursor(conf))
  def apply[T](implicit ev: ConfReads[T]): ConfReads[T] = ev
  sealed abstract class Result[+A] {
    final def fold[B](
        success: (A, List[ConfError]) => B,
        error: ConfError => B): B =
      this match {
        case Error(err) => error(err)
        case Success(ok, warnings) => success(ok, warnings)
      }
    def get: A = fold(
      (a, _) => a,
      error => throw new NoSuchElementException(error.toString)
    )
    final def toEither: Either[ConfError, A] =
      fold((a, _) => Right(a), Left.apply)
    final def toConfigured: Configured[A] =
      fold((a, _) => Configured.ok(a), Configured.notOk)
  }
  final case class Error(error: ConfError) extends Result[Nothing]
  final case class Success[+T](value: T, warnings: List[ConfError])
      extends Result[T]
  def success[T](value: T): Result[T] = Success(value, Nil)
  def error[T](err: ConfError): Result[T] = Error(err)
  def unit: Result[Unit] = success(())
}
