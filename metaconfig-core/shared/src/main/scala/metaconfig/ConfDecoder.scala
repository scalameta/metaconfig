package metaconfig

import metaconfig.Configured._
import metaconfig.Extractors.Number
import metaconfig.generic.Settings
import metaconfig.internal._

import java.nio.file.Path
import java.nio.file.Paths

import scala.collection.compat._
import scala.reflect.ClassTag

trait ConfDecoder[A] extends ConfConverter { self =>

  def read(conf: Conf): Configured[A]

  override def convert(conf: Conf): Conf = conf

  final def read(conf: Configured[Conf]): Configured[A] = conf.andThen(self.read)

  final def map[B](f: A => B): ConfDecoder[B] = new ConfDecoder[B] {
    override def read(conf: Conf): Configured[B] = self.read(conf).map(f)
    override def convert(conf: Conf): Conf = self.convert(conf)
  }

  final def flatMap[B](f: A => Configured[B]): ConfDecoder[B] =
    new ConfDecoder[B] {
      override def read(conf: Conf): Configured[B] = self.read(conf).andThen(f)
      override def convert(conf: Conf): Conf = self.convert(conf)
    }

  final def orElse(other: ConfDecoder[A]): ConfDecoder[A] = ConfDecoder
    .orElse(this, other)

  /** Fail this decoder on unknown fields.
    *
    * By default, a decoder ignores unknown fields. With .noTypos, the decoder
    * will fail if an object contains unknown fields, which typically hint the
    * user entered a typo in the config file.
    */
  final def noTypos(implicit ev: Settings[A]): ConfDecoder[A] =
    NoTyposDecoder[A](self)
}

object ConfDecoder {

  @deprecated("Use ConfDecoder[T].read instead", "0.6.1")
  def decode[T](conf: Conf)(implicit ev: ConfDecoder[T]): Configured[T] = ev
    .read(conf)

  def apply[T](implicit ev: ConfDecoder[T]): ConfDecoder[T] = ev

  // TODO(olafur) remove in favor of instanceExpect.
  @deprecated("Use fromPartial instead", "0.9.12")
  def instance[T](f: PartialFunction[Conf, Configured[T]])(implicit
      ev: ClassTag[T],
  ): ConfDecoder[T] = fromPartial(ev.runtimeClass.getName)(f)

  @deprecated("Use from instead", "0.9.12")
  def instanceF[T: ClassTag](f: Conf => Configured[T]): ConfDecoder[T] = from(f)

  def from[T](f: Conf => Configured[T]): ConfDecoder[T] = f(_)

  def fromConverted[A](
      fconv: PartialFunction[Conf, Conf],
  )(f: Conf => Configured[A]): ConfDecoder[A] = new ConfDecoder[A] {
    override def read(conf: Conf): Configured[A] = f(convert(conf))
    override def convert(conf: Conf): Conf = fconv
      .applyOrElse(conf, identity[Conf])
  }

  @deprecated("Use fromPartial instead", "0.9.12")
  def instanceExpect[T: ClassTag](expect: String)(
      f: PartialFunction[Conf, Configured[T]],
  ): ConfDecoder[T] = fromPartial(expect)(f)

  def fromPartial[A](expect: String)(
      f: PartialFunction[Conf, Configured[A]],
  ): ConfDecoder[A] = readWithPartial(expect)(f)(_)

  def fromPartialConverted[A](expect: String)(
      fconv: PartialFunction[Conf, Conf],
  )(f: PartialFunction[Conf, Configured[A]]): ConfDecoder[A] = new ConfDecoder[A] {
    override def read(conf: Conf): Configured[A] =
      readWithPartial(expect)(f)(convert(conf))
    override def convert(conf: Conf): Conf = fconv
      .applyOrElse(conf, identity[Conf])
  }

  def readWithPartial[A](
      expect: String,
  )(f: PartialFunction[Conf, Configured[A]]): Conf => Configured[A] =
    f.applyOrElse(_, (x: Conf) => NotOk(ConfError.typeMismatch(expect, x)))

  def constant[T](value: T): ConfDecoder[T] = _ => Configured.ok(value)

  implicit val confDecoder: ConfDecoder[Conf] = Configured.Ok(_)
  implicit val intConfDecoder: ConfDecoder[Int] =
    fromPartialConverted[Int]("Number") { case Conf.Str(Number(n)) => Conf.Num(n) } {
      case Conf.Num(x) => Ok(x.toInt)
    }
  implicit val bigDecimalConfDecoder: ConfDecoder[BigDecimal] =
    fromPartial[BigDecimal]("Number") { case Conf.Num(x) => Ok(x) }
  implicit val stringConfDecoder: ConfDecoder[String] =
    fromPartial[String]("String") { case Conf.Str(x) => Ok(x) }
  implicit val unitConfDecoder: ConfDecoder[Unit] = from[Unit] { case _ => Ok(()) }
  implicit val booleanConfDecoder: ConfDecoder[Boolean] =
    fromPartialConverted[Boolean]("Bool") {
      case Conf.Str("true" | "on" | "yes") => Conf.Bool(true)
      case Conf.Str("false" | "off" | "no") => Conf.Bool(false)
    } { case Conf.Bool(x) => Ok(x) }
  implicit lazy val pathConfDecoder: ConfDecoder[Path] = stringConfDecoder
    .flatMap(path => Configured.fromExceptionThrowing(Paths.get(path)))

  implicit def canBuildFromOption[A](implicit
      ev: ConfDecoder[A],
      classTag: ClassTag[A],
  ): ConfDecoder[Option[A]] = new ConfDecoder[Option[A]] {
    override def read(conf: Conf): Configured[Option[A]] = conf match {
      case Conf.Null() => Configured.ok(None)
      case _ => ev.read(conf).map(Some(_))
    }
    override def convert(conf: Conf): Conf = ev.convert(conf)
  }

  implicit def canBuildEither[A, B](implicit
      evA: ConfDecoder[A],
      evB: ConfDecoder[B],
  ): ConfDecoder[Either[A, B]] =
    orElse(evA.map(x => Left(x)), evB.map(x => Right(x)))

  // XXX: remove this method when MIMA no longer an issue
  @deprecated("Use canBuildFromAnyMapWithStringKey instead", "0.9.2")
  def canBuildFromMapWithStringKey[A: ConfDecoder: ClassTag]
      : ConfDecoder[Map[String, A]] = CanBuildFromDecoder.map[A, Map]

  implicit def canBuildFromAnyMapWithStringKey[A: ClassTag: ConfDecoder, CC[_, _]](
      implicit factory: Factory[(String, A), CC[String, A]],
  ): ConfDecoder[CC[String, A]] = CanBuildFromDecoder.map[A, CC]

  implicit def canBuildFromConfDecoder[C[_], A: ConfDecoder](implicit
      factory: Factory[A, C[A]],
      classTag: ClassTag[A],
  ): ConfDecoder[C[A]] = CanBuildFromDecoder.list[C, A]

  def orElse[A](a: ConfDecoder[A], b: ConfDecoder[A]): ConfDecoder[A] =
    new ConfDecoder[A] {
      override def read(conf: Conf): Configured[A] = a.read(conf)
        .recoverWithOrCombine(b.read(conf))
      override def convert(conf: Conf): Conf = b.convert(a.convert(conf))
    }

  implicit final class Implicits[A](private val self: ConfDecoder[A])
      extends AnyVal {

    def contramap(f: Conf => Conf): ConfDecoder[A] = new ConfDecoder[A] {
      override def read(conf: Conf): Configured[A] = self.read(f(conf))
      override def convert(conf: Conf): Conf = self.convert(f(conf))
    }

    def detectSectionRenames(implicit
        settings: generic.Settings[A],
    ): ConfDecoder[A] = SectionRenameDecoder(self)

    def withSectionRenames(renames: annotation.SectionRename*): ConfDecoder[A] =
      SectionRenameDecoder(self, renames.toList)

    def except(f: PartialFunction[Conf, Configured[A]]): ConfDecoder[A] =
      new ConfDecoder[A] {
        override def read(conf: Conf): Configured[A] = f.lift(conf)
          .getOrElse(self.read(conf))
        override def convert(conf: Conf): Conf = self.convert(conf)
      }

  }

}
