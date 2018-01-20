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

trait ConfReads[A] { self =>
  def read(cursor: Cursor): ConfReads.Result[A]
  final def map[B](f: A => B): ConfReads[B] = new ConfReads[B] {
    override def read(cursor: Cursor): ConfReads.Result[B] =
      self.read(cursor).map(f)
  }
}
object ConfReads {
  def traverse[A](cs: List[Result[A]]): Result[List[A]] = {
    cs.foldLeft(success(List.empty[A])) {
      case (res, configured) =>
        res.product(configured).map {
          case (a, b) => b :: a
        }
    }
  }
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
    final def andThen[B](f: (A, List[ConfError]) => Result[B]): Result[B] =
      fold(f, _ => this.asInstanceOf[Result[B]])
    final def map[B](f: A => B): Result[B] =
      fold(
        (a, warnings) => Success(f(a), warnings),
        _ => this.asInstanceOf[Result[B]]
      )
    def product[B](other: Result[B]): Result[(A, B)] =
      (this, other) match {
        case (Success(a, aw), Success(b, bw)) => Success(a -> b, aw ++ bw)
        case (Error(a), Error(b)) => Error(a.combine(b))
        case (Error(_), _) => this.asInstanceOf[Result[(A, B)]]
        case (_, Error(_)) => other.asInstanceOf[Result[(A, B)]]
      }
  }
  final case class Error(error: ConfError) extends Result[Nothing]
  final case class Success[+T](value: T, warnings: List[ConfError])
      extends Result[T]
  def success[T](value: T): Result[T] = Success(value, Nil)
  def error[T](err: ConfError): Result[T] = Error(err)
  def unit: Result[Unit] = success(())
}
