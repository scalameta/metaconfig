package metaconfig

import scala.collection.compat._
import scala.reflect.ClassTag
import metaconfig.Configured._
import metaconfig.Extractors.Number
import metaconfig.generic.Settings
import metaconfig.internal.CanBuildFromDecoder
import metaconfig.internal.NoTyposDecoder
import java.nio.file.Path
import java.nio.file.Paths

trait ConfDecoder[A] { self =>

  def read(conf: Conf): Configured[A]

  final def read(conf: Configured[Conf]): Configured[A] =
    conf.andThen(self.read)

  final def map[B](f: A => B): ConfDecoder[B] =
    self.flatMap(x => Ok(f(x)))
  final def orElse(other: ConfDecoder[A]): ConfDecoder[A] =
    ConfDecoder.orElse(this, other)
  final def flatMap[TT](f: A => Configured[TT]): ConfDecoder[TT] =
    self.read(_) match {
      case Ok(x) => f(x)
      case NotOk(x) => Configured.NotOk(x)
    }

  /**
    * Fail this decoder on unknown fields.
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
  def decode[T](conf: Conf)(implicit ev: ConfDecoder[T]): Configured[T] =
    ev.read(conf)

  def apply[T](implicit ev: ConfDecoder[T]): ConfDecoder[T] = ev

  // TODO(olafur) remove in favor of instanceExpect.
  @deprecated("Use fromPartial instead", "0.9.12")
  def instance[T](
      f: PartialFunction[Conf, Configured[T]]
  )(implicit ev: ClassTag[T]): ConfDecoder[T] =
    fromPartial(ev.runtimeClass.getName)(f)

  @deprecated("Use from instead", "0.9.12")
  def instanceF[T](
      f: Conf => Configured[T]
  )(implicit ev: ClassTag[T]): ConfDecoder[T] =
    from(f)

  def from[T](f: Conf => Configured[T]): ConfDecoder[T] = f(_)

  @deprecated("Use fromPartial instead", "0.9.12")
  def instanceExpect[T](expect: String)(
      f: PartialFunction[Conf, Configured[T]]
  )(implicit ev: ClassTag[T]): ConfDecoder[T] =
    fromPartial(expect)(f)

  def fromPartial[A](expect: String)(
      f: PartialFunction[Conf, Configured[A]]
  ): ConfDecoder[A] = from {
    f.applyOrElse(_, (x: Conf) => NotOk(ConfError.typeMismatch(expect, x)))
  }

  def constant[T](value: T): ConfDecoder[T] =
    _ => Configured.ok(value)

  implicit val confDecoder: ConfDecoder[Conf] =
    Configured.Ok(_)
  implicit val intConfDecoder: ConfDecoder[Int] =
    fromPartial[Int]("Number") {
      case Conf.Num(x) => Ok(x.toInt)
      case Conf.Str(Number(n)) => Ok(n.toInt)
    }
  implicit val bigDecimalConfDecoder: ConfDecoder[BigDecimal] =
    fromPartial[BigDecimal]("Number") {
      case Conf.Num(x) => Ok(x)
    }
  implicit val stringConfDecoder: ConfDecoder[String] =
    fromPartial[String]("String") { case Conf.Str(x) => Ok(x) }
  implicit val unitConfDecoder: ConfDecoder[Unit] =
    from[Unit] { case _ => Ok(()) }
  implicit val booleanConfDecoder: ConfDecoder[Boolean] =
    fromPartial[Boolean]("Bool") {
      case Conf.Bool(x) => Ok(x)
      case Conf.Str("true" | "on" | "yes") => Ok(true)
      case Conf.Str("false" | "off" | "no") => Ok(false)
    }
  implicit lazy val pathConfDecoder: ConfDecoder[Path] =
    stringConfDecoder.flatMap { path =>
      Configured.fromExceptionThrowing(Paths.get(path))
    }
  implicit def canBuildFromOption[A](
      implicit ev: ConfDecoder[A],
      classTag: ClassTag[A]
  ): ConfDecoder[Option[A]] =
    (conf: Conf) =>
      conf match {
        case Conf.Null() => Configured.ok(None)
        case _ => ev.read(conf).map(Some(_))
      }
  implicit def canBuildFromMapWithStringKey[A](
      implicit ev: ConfDecoder[A],
      classTag: ClassTag[A]
  ): ConfDecoder[Map[String, A]] =
    CanBuildFromDecoder.map[A]

  implicit def canBuildFromConfDecoder[C[_], A](
      implicit ev: ConfDecoder[A],
      factory: Factory[A, C[A]],
      classTag: ClassTag[A]
  ): ConfDecoder[C[A]] =
    CanBuildFromDecoder.list[C, A]

  def orElse[A](a: ConfDecoder[A], b: ConfDecoder[A]): ConfDecoder[A] =
    (conf: Conf) =>
      a.read(conf) match {
        case ok @ Configured.Ok(_) => ok
        case Configured.NotOk(notOk) =>
          b.read(conf) match {
            case ok2 @ Configured.Ok(_) => ok2
            case Configured.NotOk(notOk2) =>
              notOk.combine(notOk2).notOk
          }
      }
}
