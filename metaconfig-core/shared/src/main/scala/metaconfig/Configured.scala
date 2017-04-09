package metaconfig

import org.scalameta.logger

sealed abstract class Configured[+A] extends Product with Serializable {
  import Configured._
  def get: A = this match {
    case Ok(value) => value
    case NotOk(error) => throw new IllegalStateException(error.msg)
  }
  def toEither: Either[ConfError, A] = this match {
    case Ok(value) => Right(value)
    case NotOk(error) => Left(error)
  }
  def map[B](f: A => B): Configured[B] = this match {
    case Ok(value) => Ok(f(value))
    case x @ NotOk(_) => x
  }
  def product[B](other: Configured[B]): Configured[(A, B)] =
    (this, other) match {
      case (Ok(a), Ok(b)) => Ok(a -> b)
      case (NotOk(a), NotOk(b)) => NotOk(a.combine(b))
      case (NotOk(_), _) => this.asInstanceOf[Configured[(A, B)]]
      case (_, NotOk(_)) => other.asInstanceOf[Configured[(A, B)]]
    }
  def flatMap[B](f: A => Configured[B]): Configured[B] = this match {
    case Ok(value) => f(value)
    case x @ NotOk(_) => x
  }
  def isOk: Boolean = this match { case Ok(_) => true; case _ => false }
  def isNotOk: Boolean = !isOk
}
object Configured {
  def unit: Configured[Unit] = Ok(())
  case class Ok[T](value: T) extends Configured[T]
  case class NotOk(error: ConfError) extends Configured[Nothing]
}
