package metaconfig

import scala.language.experimental.macros
import scala.language.higherKinds

import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag
import metaconfig.Configured._
import metaconfig.generic.Settings
import metaconfig.internal.CanBuildFromDecoder
import metaconfig.internal.NoTyposDecoder

trait ConfDecoder[A] { self =>

  def read(conf: Conf): Configured[A]
  final def read(conf: Configured[Conf]): Configured[A] =
    conf.andThen(self.read)

  def map[B](f: A => B): ConfDecoder[B] =
    self.flatMap(x => Ok(f(x)))
  def orElse(other: ConfDecoder[A]): ConfDecoder[A] =
    ConfDecoder.orElse(this, other)
  def flatMap[TT](f: A => Configured[TT]): ConfDecoder[TT] =
    new ConfDecoder[TT] {
      override def read(any: Conf): Configured[TT] = self.read(any) match {
        case Ok(x) => f(x)
        case NotOk(x) => Configured.NotOk(x)
      }
    }

  /**
    * Fail this decoder on unknown fields.
    *
    * By default, a decoder ignores unknown fields. With .noTypos, the decoder
    * will fail if an object contains unknown fields, which typically hint the
    * user entered a typo in the config file.
    */
  def noTypos(implicit ev: Settings[A]): ConfDecoder[A] =
    NoTyposDecoder[A](self)
}

object ConfDecoder {
  def decode[T](conf: Conf)(implicit ev: ConfDecoder[T]): Configured[T] =
    ev.read(conf)
  def apply[T](implicit ev: ConfDecoder[T]): ConfDecoder[T] = ev

  // TODO(olafur) remove in favor of instanceExpect.
  def instance[T](f: PartialFunction[Conf, Configured[T]])(
      implicit ev: ClassTag[T]): ConfDecoder[T] =
    instanceExpect(ev.runtimeClass.getName)(f)
  def instanceF[T](f: Conf => Configured[T])(
      implicit ev: ClassTag[T]): ConfDecoder[T] =
    instance[T] { case x => f(x) }

  def instanceExpect[T](expect: String)(
      f: PartialFunction[Conf, Configured[T]])(
      implicit ev: ClassTag[T]): ConfDecoder[T] =
    new ConfDecoder[T] {
      override def read(any: Conf): Configured[T] =
        f.applyOrElse(
          any,
          (x: Conf) => {
            NotOk(ConfError.typeMismatch(expect, x))
          }
        )
    }

  def constant[T](value: T): ConfDecoder[T] = new ConfDecoder[T] {
    override def read(conf: Conf): Configured[T] = Configured.ok(value)
  }

  implicit val intConfDecoder: ConfDecoder[Int] =
    instanceExpect[Int]("Number") {
      case Conf.Num(x) => Ok(x.toInt)
    }
  implicit val bigDecimalConfDecoder: ConfDecoder[BigDecimal] =
    instanceExpect[BigDecimal]("Number") {
      case Conf.Num(x) => Ok(x)
    }
  implicit val stringConfDecoder: ConfDecoder[String] =
    instanceExpect[String]("String") { case Conf.Str(x) => Ok(x) }
  implicit val booleanConfDecoder: ConfDecoder[Boolean] =
    instanceExpect[Boolean]("Bool") { case Conf.Bool(x) => Ok(x) }
  implicit def canBuildFromMapWithStringKey[A](
      implicit ev: ConfDecoder[A],
      classTag: ClassTag[A]): ConfDecoder[Map[String, A]] =
    CanBuildFromDecoder.map[A]

  implicit def canBuildFromConfDecoder[C[_], A](
      implicit ev: ConfDecoder[A],
      cbf: CanBuildFrom[Nothing, A, C[A]],
      classTag: ClassTag[A]): ConfDecoder[C[A]] =
    CanBuildFromDecoder.list[C, A]

  def orElse[A](a: ConfDecoder[A], b: ConfDecoder[A]): ConfDecoder[A] =
    new ConfDecoder[A] {
      override def read(conf: Conf): Configured[A] =
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
}
