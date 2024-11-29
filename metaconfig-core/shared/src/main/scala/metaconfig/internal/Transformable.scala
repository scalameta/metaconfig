package metaconfig.internal

trait Transformable[A] { self: A =>
  type SelfType = A
  def transform(f: A => A): A
}
