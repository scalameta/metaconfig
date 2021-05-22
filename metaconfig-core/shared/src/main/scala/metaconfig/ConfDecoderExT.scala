package metaconfig

import scala.collection.compat._
import scala.reflect.ClassTag

import java.nio.file.Path
import java.nio.file.Paths

import metaconfig.internal.NoTyposDecoderEx

trait ConfDecoderExT[-S, A] {

  def read(state: Option[S], conf: Conf): Configured[A]

}

object ConfDecoderExT {

  def apply[S, A](implicit ev: ConfDecoderExT[S, A]): ConfDecoderExT[S, A] = ev

  def from[S, A](f: (Option[S], Conf) => Configured[A]): ConfDecoderExT[S, A] =
    (state, conf) => f(state, conf)

  def fromPartial[S, A](expect: String)(
      f: PartialFunction[(Option[S], Conf), Configured[A]]
  ): ConfDecoderExT[S, A] =
    (state, conf) =>
      f.applyOrElse(
        (state, conf),
        (_: (Option[S], Conf)) =>
          Configured.NotOk(ConfError.typeMismatch(expect, conf))
      )

  def constant[S, A](value: A): ConfDecoderExT[S, A] =
    (_, _) => Configured.ok(value)

  implicit def confDecoder[S]: ConfDecoderExT[S, Conf] =
    (_, conf) => Configured.Ok(conf)

  implicit def subConfDecoder[S, A <: Conf: ClassTag]: ConfDecoderExT[S, A] =
    fromPartial("Config") {
      case (_, x: A) => Configured.Ok(x)
    }

  implicit def bigDecimalConfDecoder[S]: ConfDecoderExT[S, BigDecimal] =
    fromPartial[S, BigDecimal]("Number") {
      case (_, Conf.Num(x)) => Configured.Ok(x)
      case (_, Conf.Str(Extractors.Number(n))) => Configured.Ok(n)
    }

  implicit def intConfDecoder[S]: ConfDecoderExT[S, Int] =
    bigDecimalConfDecoder[S].map(_.toInt)

  implicit def stringConfDecoder[S]: ConfDecoderExT[S, String] =
    fromPartial[S, String]("String") {
      case (_, Conf.Str(x)) => Configured.Ok(x)
    }

  implicit def unitConfDecoder[S]: ConfDecoderExT[S, Unit] =
    from[S, Unit] { case _ => Configured.unit }

  implicit def booleanConfDecoder[S]: ConfDecoderExT[S, Boolean] =
    fromPartial[S, Boolean]("Bool") {
      case (_, Conf.Bool(x)) => Configured.Ok(x)
      case (_, Conf.Str("true" | "on" | "yes")) => Configured.Ok(true)
      case (_, Conf.Str("false" | "off" | "no")) => Configured.Ok(false)
    }

  implicit def pathConfDecoder[S]: ConfDecoderExT[S, Path] =
    stringConfDecoder[S].flatMap { path =>
      Configured.fromExceptionThrowing(Paths.get(path))
    }

  implicit def canBuildOptionT[S, A](
      implicit ev: ConfDecoderExT[S, A]
  ): ConfDecoderExT[S, Option[A]] =
    (state, conf) =>
      conf match {
        case Conf.Null() => Configured.ok(None)
        case _ => ev.read(state, conf).map(Some.apply)
      }

  implicit def canBuildOption[A](
      implicit ev: ConfDecoderEx[A]
  ): ConfDecoderEx[Option[A]] =
    (state, conf) =>
      conf match {
        case Conf.Null() => Configured.ok(None)
        case _ => ev.read(state.flatten, conf).map(Some.apply)
      }

  implicit def canBuildStringMapT[S, A, CC[_, _]](
      implicit ev: ConfDecoderExT[S, A],
      factory: Factory[(String, A), CC[String, A]],
      classTag: ClassTag[A]
  ): ConfDecoderExT[S, CC[String, A]] =
    fromPartial(
      s"Map[String, ${classTag.runtimeClass.getName}]"
    ) {
      case (state, Conf.Obj(values)) =>
        buildFrom(state, values, ev, factory)(_._2, (x, y) => (x._1, y))
    }

  implicit def canBuildStringMap[A, CC[x, y] <: collection.Iterable[(x, y)]](
      implicit ev: ConfDecoderEx[A],
      factory: Factory[(String, A), CC[String, A]],
      classTag: ClassTag[A]
  ): ConfDecoderEx[CC[String, A]] = {
    val none: Option[A] = None
    fromPartial(
      s"Map[String, ${classTag.runtimeClass.getName}]"
    ) {
      case (stateOpt, Conf.Obj(List(("+", Conf.Obj(values))))) =>
        val res =
          buildFrom(none, values, ev, factory)(_._2, (x, y) => (x._1, y))
        res.map { x =>
          stateOpt.fold(x) { state =>
            val builder = factory.newBuilder
            builder ++= state
            builder ++= x
            builder.result()
          }
        }
      case (_, Conf.Obj(values)) =>
        buildFrom(none, values, ev, factory)(_._2, (x, y) => (x._1, y))
    }
  }

  implicit def canBuildSeqT[S, A, C[_]](
      implicit ev: ConfDecoderExT[S, A],
      factory: Factory[A, C[A]],
      classTag: ClassTag[A]
  ): ConfDecoderExT[S, C[A]] =
    fromPartial(
      s"List[${classTag.runtimeClass.getName}]"
    ) {
      case (state, Conf.Lst(values)) =>
        buildFrom(state, values, ev, factory)(identity, (_, x) => x)
    }

  implicit def canBuildSeq[A, C[x] <: collection.Iterable[x]](
      implicit ev: ConfDecoderEx[A],
      factory: Factory[A, C[A]],
      classTag: ClassTag[A]
  ): ConfDecoderEx[C[A]] = {
    val none: Option[A] = None
    fromPartial(
      s"List[${classTag.runtimeClass.getName}]"
    ) {
      case (stateOpt, Conf.Obj(List(("+", Conf.Lst(values))))) =>
        buildFrom(none, values, ev, factory)(identity, (_, x) => x).map { x =>
          stateOpt.fold(x) { state =>
            val builder = factory.newBuilder
            builder ++= state
            builder ++= x
            builder.result()
          }
        }
      case (_, Conf.Lst(values)) =>
        buildFrom(none, values, ev, factory)(identity, (_, x) => x)
    }
  }

  implicit final class Implicits[S, A](self: ConfDecoderExT[S, A]) {

    def read(state: Option[S], conf: Configured[Conf]): Configured[A] =
      conf.andThen(self.read(state, _))

    def map[B](f: A => B): ConfDecoderExT[S, B] =
      (state, conf) => self.read(state, conf).map(f)

    def flatMap[B](f: A => Configured[B]): ConfDecoderExT[S, B] =
      (state, conf) => self.read(state, conf).andThen(f)

    def orElse(other: ConfDecoderExT[S, A]): ConfDecoderExT[S, A] =
      (state, conf) =>
        self.read(state, conf).recoverWith { x =>
          other.read(state, conf).recoverWith(x.combine)
        }

    def noTypos(implicit settings: generic.Settings[A]): ConfDecoderExT[S, A] =
      if (self.isInstanceOf[NoTyposDecoderEx[_, _]]) self
      else new NoTyposDecoderEx[S, A](self)

  }

  private[metaconfig] def buildFrom[V, S, A, B, Coll](
      state: Option[S],
      values: List[V],
      ev: ConfDecoderExT[S, A],
      factory: Factory[B, Coll]
  )(a2conf: V => Conf, ab2c: (V, A) => B): Configured[Coll] = {
    val successB = factory.newBuilder
    val errorB = List.newBuilder[ConfError]
    successB.sizeHint(values.length)
    values.foreach { value =>
      val res = ev.read(state, a2conf(value))
      res.foreach(errorB += _)(successB += ab2c(value, _))
    }
    Configured(successB.result(), errorB.result(): _*)
  }

}

object ConfDecoderEx {

  def apply[A](implicit ev: ConfDecoderEx[A]): ConfDecoderEx[A] = ev

  def from[A](f: (Option[A], Conf) => Configured[A]): ConfDecoderEx[A] =
    ConfDecoderExT.from[A, A](f)

  def fromPartial[A](expect: String)(
      f: PartialFunction[(Option[A], Conf), Configured[A]]
  ): ConfDecoderEx[A] = ConfDecoderExT.fromPartial[A, A](expect)(f)

}
