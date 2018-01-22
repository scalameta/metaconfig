package metaconfig

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag

/**
  * Metadata about one field of a class.
  *
  * @param name the parameter name of this field.
  * @param tpe the pretty-printed type of this parameter
  * @param annotations static annotations attached to this field.
  */
final class Field(
    val name: String,
    val tpe: String,
    val annotations: List[StaticAnnotation]
) {
  override def toString: String = {
    val annots = annotations.map(annot => s"@$annot").mkString(", ")
    s"""Field(name="$name",tpe="$tpe",annotations=List($annots))"""
  }
}

/**
  * Aggregated metadata about a given type.
  *
  * @param fields the fields of this type
  * @tparam T not used for anything but to drive implicit resolution.
  */
final class Surface[T](val fields: List[List[Field]]) {
  override def toString: String = s"Surface($fields)"
  def this(fields: List[Field]*) =
    this(fields.toList)
}
object Surface {
  def apply[T](implicit ev: Surface[T]): Surface[T] = ev
}
