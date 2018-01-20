package metaconfig

case class Surface[T](fields: List[Field], factory: ObjectFactory[T])

object Surface {
  def apply[T](implicit ev: Surface[T]): Surface[T] = ev
}
