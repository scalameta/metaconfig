package metaconfig

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag

/**
  * Metadata about one field of a class.
  *
  * @param name the parameter name of this field.
  * @param defaultValue the default value of this parameter, if any
  * @param classTag the classtag of the the type of this field
  * @param annotations static annotations attached to this field.
  */
final case class Field(
    name: String,
    defaultValue: Option[DefaultValue[_]],
    classTag: ClassTag[_],
    annotations: List[StaticAnnotation]
)

/**
  * Aggregated metadata about a given type.
  *
  * @param fields the fields of this type
  * @tparam T not used for anything but to drive implicit resolution.
  */
case class Surface[T](fields: List[Field])
object Surface {
  def apply[T](implicit ev: Surface[T]): Surface[T] = ev
}
