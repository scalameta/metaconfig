package metaconfig

import scala.util.Failure
import scala.util.Success

sealed abstract class Configured[+A] extends Product with Serializable {
  import Configured._
  def getOrElse[B >: A](els: => B): B = this match {
    case Ok(value) => value
    case NotOk(_) => els
  }
  def get: A = this match {
    case Ok(value) => value
    case NotOk(error) =>
      throw new NoSuchElementException(error.toString)
  }

  def orElse[B >: A](alternative: => Configured[B]): Configured[B] =
    this match {
      case _: NotOk => alternative
      case ok => ok
    }
  def recoverWith[B >: A](f: NotOk => Configured[B]): Configured[B] =
    this match {
      case x: NotOk => f(x)
      case ok => ok
    }
  def toEither: Either[ConfError, A] = this match {
    case Ok(value) => Right(value)
    case NotOk(error) => Left(error)
  }
  def map[B](f: A => B): Configured[B] = this match {
    case Ok(value) => Ok(f(value))
    case x @ NotOk(_) => x
  }
  def |@|[B](other: Configured[B]): Configured[(A, B)] = this.product(other)
  def product[B](other: Configured[B]): Configured[(A, B)] =
    (this, other) match {
      case (Ok(a), Ok(b)) => Ok(a -> b)
      case (a: NotOk, b: NotOk) => a.combine(b)
      case (NotOk(_), _) => this.asInstanceOf[Configured[(A, B)]]
      case (_, NotOk(_)) => other.asInstanceOf[Configured[(A, B)]]
    }
  def andThen[B](f: A => Configured[B]): Configured[B] = this match {
    case Ok(value) => f(value)
    case x @ NotOk(_) => x
  }
  def isOk: Boolean = this match { case Ok(_) => true; case _ => false }
  def isNotOk: Boolean = !isOk
}

trait ConfiguredLowPriorityImplicits {

  // implicit def toOption[A](value: Configured[A]): Option[A] =
  //   value match {
  //     case Configured.Ok(v) => Some(v)
  //     case _ => None
  //   }

}

object Configured extends ConfiguredLowPriorityImplicits {
  def apply[T](value: => T, error: Option[ConfError]): Configured[T] =
    error.fold(ok(value))(notOk)
  def apply[T](value: => T, errors: ConfError*): Configured[T] =
    apply(value, ConfError(errors))
  def opt[T](value: Option[T])(error: => ConfError): Configured[T] =
    value.fold(notOk[T](error))(ok)

  @deprecated("No longer supported", "0.8.1")
  def traverse[T](cs: List[Configured[T]]): Configured[List[T]] = {
    cs.foldLeft(ok(List.empty[T])) {
      case (res, configured) =>
        res.product(configured).map { case (a, b) => b :: a }
    }
  }
  def unit: Configured[Unit] = Ok(())
  def ok[T](e: T): Configured[T] = Ok(e)
  def notOk[T](error: ConfError): Configured[T] = NotOk(error)
  def error(message: String): Configured[Nothing] =
    ConfError.message(message).notOk
  def fromExceptionThrowing[T](thunk: => T): Configured[T] = {
    scala.util.Try(thunk) match {
      case Failure(exception) => Configured.exception(exception)
      case Success(value) => Configured.ok(value)
    }
  }
  def exception(
      exception: Throwable,
      stackSize: Int = 10
  ): Configured[Nothing] =
    ConfError.exception(exception, stackSize).notOk
  def typeMismatch(expected: String, obtained: Conf): Configured[Nothing] =
    ConfError.typeMismatch(expected, obtained).notOk
  def missingField(obj: Conf.Obj, field: String): Configured[Nothing] =
    ConfError.missingField(obj, field).notOk
  final case class Ok[T](value: T) extends Configured[T]
  final case class NotOk(error: ConfError) extends Configured[Nothing] {
    def combine(other: ConfError): NotOk = NotOk(error.combine(other))
    @inline
    def combine(other: NotOk): NotOk = combine(other.error)
  }

  implicit class ConfiguredImplicit[A](value: Configured[A]) {

    def getOrRecover(fa: ConfError => A): A =
      fold(fa)(identity)

    def fold[B](fa: ConfError => B)(fb: A => B): B =
      value match {
        case Configured.Ok(value) => fb(value)
        case Configured.NotOk(error) => fa(error)
      }

    def foreach(fa: ConfError => Unit)(fb: A => Unit): Unit =
      fold(fa)(fb)

  }

}
