package metaconfig

import scala.language.experimental.macros

import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag
import scala.reflect.macros.blackbox
import metaconfig.Configured._
import org.scalameta.logger

trait HasFields {
  def fields: Map[String, Any]
}

trait ConfDecoder[A] { self =>
  def read(conf: Conf): Configured[A]
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
}

object ConfDecoder {


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
    instanceExpect[Map[String, A]](
      s"Map[String, ${classTag.runtimeClass.getName}]") {
      case Conf.Obj(values) =>
        val results = values.map {
          case (key, value) => ev.read(value).map(key -> _)
        }
        ConfError.fromResults(results) match {
          case Some(err) => NotOk(err)
          case None =>
            Ok(results.collect { case Configured.Ok(x) => x }.toMap)
        }
    }

  import scala.language.higherKinds
  implicit def canBuildFromConfDecoder[C[_], A](
      implicit ev: ConfDecoder[A],
      cbf: CanBuildFrom[Nothing, A, C[A]],
      classTag: ClassTag[A]): ConfDecoder[C[A]] =
    new ConfDecoder[C[A]] {
      override def read(conf: Conf): Configured[C[A]] = conf match {
        case Conf.Lst(values) =>
          val successB = cbf()
          val errorB = List.newBuilder[ConfError]
          successB.sizeHint(values.length)
          values.foreach { value =>
            ev.read(value) match {
              case NotOk(e) => errorB += e
              case Ok(e) => successB += e
            }
          }
          ConfError(errorB.result()) match {
            case Some(x) => NotOk(x)
            case None => Ok(successB.result())
          }
        case _ =>
          val error = ConfError.typeMismatch(
            s"List[${classTag.runtimeClass.getName}]",
            conf
          )
          NotOk(error)
      }
    }
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
