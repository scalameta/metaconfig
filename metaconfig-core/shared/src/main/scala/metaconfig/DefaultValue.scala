package metaconfig

case class DefaultValue[+T](value: T, show: () => String)

object DefaultValue {
  def apply[T](e: T)(implicit ev: DefaultValueShow[T]): DefaultValue[T] = {
    DefaultValue(e, () => ev.show(e))
  }
}
trait DefaultValueShow[T] {
  def show(e: T): String
}

trait LowPriorityDefaultValueShow {
  lazy val DefaultValueShowAny: DefaultValueShow[Any] =
    new DefaultValueShow[Any] {
      override def show(e: Any): String = e.toString
    }
  implicit def DefaultValueShowToString[T]: DefaultValueShow[T] =
    DefaultValueShowAny.asInstanceOf[DefaultValueShow[T]]
}

object DefaultValueShow extends LowPriorityDefaultValueShow
