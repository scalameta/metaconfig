package metaconfig.error

final case class CompositeException(head: Throwable, tail: List[Throwable])
    extends Exception(head.getMessage) {
  def all: List[Throwable] = head :: tail
  def add(other: Throwable): CompositeException = other match {
    case composite @ CompositeException(_, _) =>
      CompositeException(head, tail ++ composite.all)
    case _ => CompositeException(other, head :: tail)
  }
  override def getCause: Throwable = head
}

object CompositeException {
  def apply(a: Throwable, b: Throwable): CompositeException = (a, b) match {
    case (c: CompositeException, other) => c.add(other)
    case (other, c: CompositeException) => c.add(other)
    case _ => CompositeException(a, b :: Nil)
  }
}
