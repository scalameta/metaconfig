package metaconfig

import scala.reflect.ClassTag
import scala.util.control.NonFatal

trait ObjectFactory[T] {
  final def newInstance(args: List[List[Any]]): Either[String, T] =
    try Right(unsafeNewInstance(args))
    catch {
      case NonFatal(cast) =>
        Left(cast.getMessage)
    }
  def unsafeNewInstance(args: List[List[Any]]): T
}

object ObjectFactory {
  def cast[T: ClassTag](field: String, expectedType: String, value: Any): T =
    value match {
      case t: T => t
      case _ =>
        throw new IllegalArgumentException(
          ConfError.typeMismatch(expectedType, value.toString, field).msg
        )
    }
}
