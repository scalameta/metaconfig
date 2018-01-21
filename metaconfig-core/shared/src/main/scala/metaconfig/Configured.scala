package metaconfig

sealed abstract class Configured[+A] extends Product with Serializable {
  import Configured._
  def getOrElse[B >: A](els: => B): B = this match {
    case Ok(value) => value
    case NotOk(_) => els
  }
  def get: A = this match {
    case Ok(value) => value
    case NotOk(error) => throw new NoSuchElementException(error.toString)
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
      case (NotOk(a), NotOk(b)) => NotOk(a.combine(b))
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
object Configured {
  // TODO(olafur) start using cats or scalaz...
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
  def exception(
      exception: Throwable,
      stackSize: Int = 10): Configured[Nothing] =
    ConfError.exception(exception, stackSize).notOk
  def typeMismatch(expected: String, obtained: Conf): Configured[Nothing] =
    ConfError.typeMismatch(expected, obtained).notOk
  def missingField(obj: Conf.Obj, field: String): Configured[Nothing] =
    ConfError.missingField(obj, field).notOk
  final case class Ok[T](value: T) extends Configured[T]
  final case class NotOk(error: ConfError) extends Configured[Nothing] {
    def combine(other: ConfError): NotOk = NotOk(error.combine(other))
  }
}
