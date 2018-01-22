package metaconfig.generic

import scala.annotation.StaticAnnotation

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
    val annotations: List[StaticAnnotation],
    val underlying: List[List[Field]]
) {
  def withName(newName: String) =
    new Field(newName, tpe, annotations, underlying)

  /**
    * Returns this field with all underlying fields expaneded.
    *
    * Underlying field names become prefixed by their enclosing fields.
    */
  def flat: List[Field] = {
    if (underlying.isEmpty) this :: Nil
    else {
      for {
        fields <- underlying
        field <- fields
        flattened <- field.withName(s"$name.${field.name}").flat
      } yield flattened
    }
  }
  override def toString: String = {
    val annots = annotations.map(annot => s"@$annot").mkString(", ")
    s"""Field(name="$name",tpe="$tpe",annotations=List($annots),underlying=$underlying)"""
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
