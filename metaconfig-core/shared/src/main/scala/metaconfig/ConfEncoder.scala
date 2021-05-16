package metaconfig

import java.nio.file.Path

trait ConfEncoder[A] { self =>
  def write(value: A): Conf
  final def writeObj(value: A): Conf.Obj =
    write(value) match {
      case o: Conf.Obj => o
      case els => ConfError.typeMismatch("Conf.Obj", els).notOk.get
    }

  final def contramap[B](f: B => A): ConfEncoder[B] =
    (value: B) => self.write(f(value))
}

object ConfEncoder {
  def empty[A]: ConfEncoder[A] = instance[A](_ => Conf.Null())

  def apply[A](implicit ev: ConfEncoder[A]): ConfEncoder[A] = ev

  def instance[A](f: A => Conf): ConfEncoder[A] = f(_)

  private val GenericConfEncoder: ConfEncoder[Conf] =
    instance(identity)

  // Invariant type-classes don't work unfortunately so we do this work by hand.
  implicit def ConfEncoder[T <: Conf]: ConfEncoder[T] =
    GenericConfEncoder.asInstanceOf[ConfEncoder[T]]

  implicit val BooleanEncoder: ConfEncoder[Boolean] =
    Conf.Bool(_)

  implicit val IntEncoder: ConfEncoder[Int] =
    Conf.Num(_)

  implicit val UnitEncoder: ConfEncoder[Unit] =
    _ => Conf.Null()

  implicit val StringEncoder: ConfEncoder[String] =
    Conf.Str(_)

  implicit lazy val PathEncoder: ConfEncoder[Path] =
    (value: Path) => Conf.Str(value.toString())

  implicit def IterableEncoder[A, C[x] <: Iterable[x]](
      implicit ev: ConfEncoder[A]
  ): ConfEncoder[C[A]] =
    (value: C[A]) => Conf.Lst(value.view.map(ev.write).toList)

  implicit def OptionEncoder[A](
      implicit ev: ConfEncoder[A]
  ): ConfEncoder[Option[A]] =
    _.fold[Conf](Conf.Null())(ev.write)

  implicit def MapEncoder[A, CC[String, A] <: collection.Map[String, A]](
      implicit ev: ConfEncoder[A]
  ): ConfEncoder[CC[String, A]] =
    (value: CC[String, A]) =>
      Conf.Obj(value.view.map { case (k, v) => k -> ev.write(v) }.toList)

}
