package mopt.generic

import scala.annotation.StaticAnnotation

/**
  * Aggregated metadata about a given type.
  *
  * @param fields the fields of this type
  * @tparam T not used for anything but to drive implicit resolution.
  */
final class Surface[T](
    val fields: List[List[Field]],
    val annotations: List[StaticAnnotation]
) {
  def this(fields: List[List[Field]]) = this(fields, Nil)
  def cast[B]: Surface[B] = new Surface(fields)
  override def toString: String = s"Surface($fields)"
}
object Surface {
  implicit val unitSurface: Surface[Unit] = empty[Unit]
  def empty[T]: Surface[T] = new Surface(Nil)
  def apply[T](implicit ev: Surface[T]): Surface[T] = ev
}
