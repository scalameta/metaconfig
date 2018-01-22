package metaconfig

@deprecated("Copy paste the implementation to your project", "0.6")
object String2AnyMap {
  def unapply(arg: Any): Option[Map[String, Any]] = arg match {
    case someMap: Map[_, _] =>
      try {
        Some(someMap.asInstanceOf[Map[String, Any]])
      } catch {
        case _: ClassCastException =>
          None
      }
    case _ => None
  }
}
