package metaconfig

import metaconfig.internal._

import java.nio.file.Path
import java.nio.file.Paths

import scala.collection.compat._
import scala.reflect.ClassTag

trait ConfDecoderExT[-S, A] {

  def read(state: Option[S], conf: Conf): Configured[A]

}

object ConfDecoderExT {

  def apply[S, A](implicit ev: ConfDecoderExT[S, A]): ConfDecoderExT[S, A] = ev

  def from[S, A](f: (Option[S], Conf) => Configured[A]): ConfDecoderExT[S, A] =
    (state, conf) => f(state, conf)

  def fromPartial[S, A](expect: String)(
      f: PartialFunction[(Option[S], Conf), Configured[A]],
  ): ConfDecoderExT[S, A] = (state, conf) =>
    f.applyOrElse(
      (state, conf),
      (_: (Option[S], Conf)) =>
        Configured.NotOk(ConfError.typeMismatch(expect, conf)),
    )

  def constant[S, A](value: A): ConfDecoderExT[S, A] =
    (_, _) => Configured.ok(value)

  implicit def confDecoder[S]: ConfDecoderExT[S, Conf] =
    (_, conf) => Configured.Ok(conf)

  implicit def subConfDecoder[S, A <: Conf: ClassTag]: ConfDecoderExT[S, A] =
    fromPartial("Config") { case (_, x: A) => Configured.Ok(x) }

  implicit def bigDecimalConfDecoder[S]: ConfDecoderExT[S, BigDecimal] =
    fromPartial[S, BigDecimal]("Number") {
      case (_, Conf.Num(x)) => Configured.Ok(x)
      case (_, Conf.Str(Extractors.Number(n))) => Configured.Ok(n)
    }

  implicit def intConfDecoder[S]: ConfDecoderExT[S, Int] =
    bigDecimalConfDecoder[S].map(_.toInt)

  implicit def stringConfDecoder[S]: ConfDecoderExT[S, String] =
    fromPartial[S, String]("String") { case (_, Conf.Str(x)) => Configured.Ok(x) }

  implicit def unitConfDecoder[S]: ConfDecoderExT[S, Unit] =
    from[S, Unit] { case _ => Configured.unit }

  implicit def booleanConfDecoder[S]: ConfDecoderExT[S, Boolean] =
    fromPartial[S, Boolean]("Bool") {
      case (_, Conf.Bool(x)) => Configured.Ok(x)
      case (_, Conf.Str("true" | "on" | "yes")) => Configured.Ok(true)
      case (_, Conf.Str("false" | "off" | "no")) => Configured.Ok(false)
    }

  implicit def pathConfDecoder[S]: ConfDecoderExT[S, Path] = stringConfDecoder[S]
    .flatMap(path => Configured.fromExceptionThrowing(Paths.get(path)))

  implicit def canBuildOptionT[S, A](implicit
      ev: ConfDecoderExT[S, A],
  ): ConfDecoderExT[S, Option[A]] = new ConfDecoderExT[S, Option[A]] {
    override def read(state: Option[S], conf: Conf): Configured[Option[A]] =
      conf match {
        case Conf.Null() => Configured.ok(None)
        case _ => ev.read(state, conf).map(Some.apply)
      }
  }

  implicit def canBuildOption[A](implicit
      ev: ConfDecoderEx[A],
  ): ConfDecoderEx[Option[A]] = new ConfDecoderEx[Option[A]] {
    override def read(
        state: Option[Option[A]],
        conf: Conf,
    ): Configured[Option[A]] = conf match {
      case Conf.Null() => Configured.ok(None)
      case _ => ev.read(state.flatten, conf).map(Some.apply)
    }
  }

  implicit def canBuildEitherT[S, A, B](implicit
      evA: ConfDecoderExT[S, A],
      evB: ConfDecoderExT[S, B],
  ): ConfDecoderExT[S, Either[A, B]] = evA.map[Either[A, B]](Left.apply)
    .orElse(evB.map[Either[A, B]](Right.apply))

  implicit def canBuildEither[A, B](implicit
      evA: ConfDecoderEx[A],
      evB: ConfDecoderEx[B],
  ): ConfDecoderEx[Either[A, B]] = new ConfDecoderEx[Either[A, B]] {
    override def read(
        state: Option[Either[A, B]],
        conf: Conf,
    ): Configured[Either[A, B]] = {
      @inline
      def asA(s: Option[A]) = evA.read(s, conf).map(x => Left(x))
      @inline
      def asB(s: Option[B]) = evB.read(s, conf).map(x => Right(x))
      state.fold(asA(None).recoverWithOrCombine(asB(None))) {
        _.fold(
          a => asA(Some(a)).recoverWithOrCombine(asB(None)),
          b => asB(Some(b)).recoverWithOrCombine(asA(None)),
        )
      }
    }
  }

  implicit def canBuildStringMapT[S, A, CC[_, _]](implicit
      ev: ConfDecoderExT[S, A],
      factory: Factory[(String, A), CC[String, A]],
      classTag: ClassTag[A],
  ): ConfDecoderExT[S, CC[String, A]] = new ConfDecoderExT[S, CC[String, A]] {
    override def read(state: Option[S], conf: Conf): Configured[CC[String, A]] =
      conf match {
        case Conf.Obj(values) =>
          buildFrom(state, values, ev, factory)(_._2, (x, y) => (x._1, y))
        case _ =>
          val expect = s"Map[String, ${classTag.runtimeClass.getName}]"
          Configured.NotOk(ConfError.typeMismatch(expect, conf))
      }
  }

  implicit def canBuildStringMap[A, CC[x, y] <: collection.Iterable[(x, y)]](
      implicit
      ev: ConfDecoderEx[A],
      factory: Factory[(String, A), CC[String, A]],
      classTag: ClassTag[A],
  ): ConfDecoderEx[CC[String, A]] = new ConfDecoderEx[CC[String, A]] {
    val none: Option[A] = None
    override def read(
        state: Option[CC[String, A]],
        conf: Conf,
    ): Configured[CC[String, A]] = conf match {
      case Conf.Obj(List(("+", Conf.Obj(values)))) =>
        buildFrom(none, values, ev, factory)(_._2, (x, y) => (x._1, y)).map { x =>
          state.fold(x) { state =>
            val builder = factory.newBuilder
            builder ++= state
            builder ++= x
            builder.result()
          }
        }
      case Conf.Obj(values) =>
        buildFrom(none, values, ev, factory)(_._2, (x, y) => (x._1, y))
      case _ =>
        val expect = s"Map[String, ${classTag.runtimeClass.getName}]"
        Configured.NotOk(ConfError.typeMismatch(expect, conf))
    }
  }

  implicit def canBuildSeqT[S, A, C[_]](implicit
      ev: ConfDecoderExT[S, A],
      factory: Factory[A, C[A]],
      classTag: ClassTag[A],
  ): ConfDecoderExT[S, C[A]] = new ConfDecoderExT[S, C[A]] {
    override def read(state: Option[S], conf: Conf): Configured[C[A]] =
      conf match {
        case Conf.Lst(values) =>
          buildFrom(state, values, ev, factory)(identity, (_, x) => x)
        case _ =>
          val expect = s"List[${classTag.runtimeClass.getName}]"
          Configured.NotOk(ConfError.typeMismatch(expect, conf))
      }
  }

  implicit def canBuildSeq[A, C[x] <: collection.Iterable[x]](implicit
      ev: ConfDecoderEx[A],
      factory: Factory[A, C[A]],
      classTag: ClassTag[A],
  ): ConfDecoderEx[C[A]] = new ConfDecoderEx[C[A]] {
    private val none: Option[A] = None
    override def read(state: Option[C[A]], conf: Conf): Configured[C[A]] =
      conf match {
        case Conf.Obj(List(("+", Conf.Lst(values)))) =>
          buildFrom(none, values, ev, factory)(identity, (_, x) => x).map { x =>
            state.fold(x) { state =>
              val builder = factory.newBuilder
              builder ++= state
              builder ++= x
              builder.result()
            }
          }
        case Conf.Lst(values) =>
          buildFrom(none, values, ev, factory)(identity, (_, x) => x)
        case _ =>
          val expect = s"List[${classTag.runtimeClass.getName}]"
          Configured.NotOk(ConfError.typeMismatch(expect, conf))
      }
  }

  implicit final class Implicits[S, A](private val self: ConfDecoderExT[S, A])
      extends AnyVal {

    def read(state: Option[S], conf: Configured[Conf]): Configured[A] = conf
      .andThen(self.read(state, _))

    def map[B](f: A => B): ConfDecoderExT[S, B] = new ConfDecoderExT[S, B] {
      override def read(state: Option[S], conf: Conf): Configured[B] = self
        .read(state, conf).map(f)
    }

    def flatMap[B](f: A => Configured[B]): ConfDecoderExT[S, B] =
      new ConfDecoderExT[S, B] {
        override def read(state: Option[S], conf: Conf): Configured[B] = self
          .read(state, conf).andThen(f)
      }

    def orElse(other: ConfDecoderExT[S, A]): ConfDecoderExT[S, A] =
      new ConfDecoderExT[S, A] {
        override def read(state: Option[S], conf: Conf): Configured[A] = self
          .read(state, conf).recoverWithOrCombine(other.read(state, conf))
      }

    def noTypos(implicit settings: generic.Settings[A]): ConfDecoderExT[S, A] =
      NoTyposDecoder(self)

    def detectSectionRenames(implicit
        settings: generic.Settings[A],
    ): ConfDecoderExT[S, A] = SectionRenameDecoder(self)

    def withSectionRenames(
        renames: annotation.SectionRename*,
    ): ConfDecoderExT[S, A] = SectionRenameDecoder(self, renames.toList)

  }

  private[metaconfig] def buildFrom[V, S, A, B, Coll](
      state: Option[S],
      values: List[V],
      ev: ConfDecoderExT[S, A],
      factory: Factory[B, Coll],
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
      f: PartialFunction[(Option[A], Conf), Configured[A]],
  ): ConfDecoderEx[A] = ConfDecoderExT.fromPartial[A, A](expect)(f)

}
