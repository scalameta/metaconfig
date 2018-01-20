package metaconfig

case class Surface[T](fields: List[Field])

object Surface {
  def apply[T](implicit ev: Surface[T]): Surface[T] = ev
}
