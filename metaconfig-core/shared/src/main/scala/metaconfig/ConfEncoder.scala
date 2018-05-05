package metaconfig

import scala.language.higherKinds

trait ConfEncoder[A] { self =>
  def write(value: A): Conf

  final def contramap[B](f: B => A): ConfEncoder[B] = new ConfEncoder[B] {
    override def write(value: B): Conf = self.write(f(value))
  }
}

object ConfEncoder {

  def apply[A](implicit ev: ConfEncoder[A]): ConfEncoder[A] = ev

  def instance[A](f: A => Conf): ConfEncoder[A] = new ConfEncoder[A] {
    override def write(value: A): Conf = f(value)
  }

  implicit val BooleanEncoder: ConfEncoder[Boolean] =
    new ConfEncoder[Boolean] {
      override def write(value: Boolean): Conf = Conf.Bool(value)
    }

  implicit val IntEncoder: ConfEncoder[Int] =
    new ConfEncoder[Int] {
      override def write(value: Int): Conf = Conf.Num(value)
    }

  implicit val StringEncoder: ConfEncoder[String] =
    new ConfEncoder[String] {
      override def write(value: String): Conf = Conf.Str(value)
    }

  implicit def SeqEncoder[A, C[x] <: Seq[x]](
      implicit ev: ConfEncoder[A]): ConfEncoder[C[A]] =
    new ConfEncoder[C[A]] {
      override def write(value: C[A]): Conf = {
        Conf.Lst(value.iterator.map(ev.write).toList)
      }
    }

  implicit def MapEncoder[A](
      implicit ev: ConfEncoder[A]): ConfEncoder[Map[String, A]] =
    new ConfEncoder[Map[String, A]] {
      override def write(value: Map[String, A]): Conf = {
        Conf.Obj(value.mapValues(ev.write).toList)
      }
    }

}
