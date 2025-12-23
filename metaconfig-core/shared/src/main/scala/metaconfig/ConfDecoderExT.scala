package metaconfig

import metaconfig.internal._

import java.nio.file.Path
import java.nio.file.Paths

import scala.collection.compat._
import scala.reflect.ClassTag

trait ConfDecoderExT[-S, A] extends ConfConverter {

  def read(state: Option[S], conf: Conf): Configured[A]
  override def convert(conf: Conf): Conf = conf

}

object ConfDecoderExT {

  def apply[S, A](implicit ev: ConfDecoderExT[S, A]): ConfDecoderExT[S, A] = ev

  def from[S, A](f: (Option[S], Conf) => Configured[A]): ConfDecoderExT[S, A] =
    (state, conf) => f(state, conf)

  def fromConverted[S, A](fconv: PartialFunction[Conf, Conf])(
      f: (Option[S], Conf) => Configured[A],
  ): ConfDecoderExT[S, A] = new ConfDecoderExT[S, A] {
    override def read(state: Option[S], conf: Conf): Configured[A] =
      f(state, convert(conf))
    override def convert(conf: Conf): Conf = fconv
      .applyOrElse(conf, identity[Conf])
  }

  def fromPartial[S, A](expect: String)(
      f: PartialFunction[(Option[S], Conf), Configured[A]],
  ): ConfDecoderExT[S, A] =
    fromPartialConverted[S, A](expect)(PartialFunction.empty)(f)

  def fromPartialConverted[S, A](
      expect: String,
  )(fconv: PartialFunction[Conf, Conf])(
      f: PartialFunction[(Option[S], Conf), Configured[A]],
  ): ConfDecoderExT[S, A] = new ConfDecoderExT[S, A] {
    override def read(state: Option[S], conf: Conf): Configured[A] = f
      .applyOrElse(
        (state, convert(conf)),
        (_: (Option[S], Conf)) =>
          Configured.NotOk(ConfError.typeMismatch(expect, conf)),
      )
    override def convert(conf: Conf): Conf = fconv
      .applyOrElse(conf, identity[Conf])
  }

  def constant[S, A](value: A): ConfDecoderExT[S, A] =
    (_, _) => Configured.ok(value)

  implicit def confDecoder[S]: ConfDecoderExT[S, Conf] =
    (_, conf) => Configured.Ok(conf)

  implicit def subConfDecoder[S, A <: Conf: ClassTag]: ConfDecoderExT[S, A] =
    fromPartial("Config") { case (_, x: A) => Configured.Ok(x) }

  implicit def bigDecimalConfDecoder[S]: ConfDecoderExT[S, BigDecimal] =
    fromPartialConverted[S, BigDecimal]("Number") {
      case Conf.Str(Extractors.Number(n)) => Conf.Num(n)
    } { case (_, Conf.Num(x)) => Configured.Ok(x) }

  implicit def intConfDecoder[S]: ConfDecoderExT[S, Int] =
    bigDecimalConfDecoder[S].map(_.toInt)

  implicit def stringConfDecoder[S]: ConfDecoderExT[S, String] =
    fromPartial[S, String]("String") { case (_, Conf.Str(x)) => Configured.Ok(x) }

  implicit def unitConfDecoder[S]: ConfDecoderExT[S, Unit] =
    from[S, Unit] { case _ => Configured.unit }

  implicit def booleanConfDecoder[S]: ConfDecoderExT[S, Boolean] =
    fromPartialConverted[S, Boolean]("Bool") {
      case Conf.Str("true" | "on" | "yes") => Conf.Bool(true)
      case Conf.Str("false" | "off" | "no") => Conf.Bool(false)
    } { case (_, Conf.Bool(x)) => Configured.Ok(x) }

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
    override def convert(conf: Conf): Conf = ev.convert(conf)
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
    override def convert(conf: Conf): Conf = ev.convert(conf)
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

    override def convert(conf: Conf): Conf = evB.convert(evA.convert(conf))
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

    override def convert(conf: Conf): Conf = conf match {
      case v: Conf.Obj => CanBuildFromDecoder.convertMap(v, ev)
      case _ => conf
    }
  }

  implicit def canBuildStringMap[A, CC[x, y] <: collection.Iterable[(x, y)]](
      implicit
      ev: ConfDecoderEx[A],
      factory: Factory[(String, A), CC[String, A]],
      classTag: ClassTag[A],
  ): ConfDecoderEx[CC[String, A]] = new ConfDecoderEx[CC[String, A]] {
    def decode(obj: Conf.Obj) =
      buildFrom(None, obj.values, ev, factory)(_._2, (x, y) => (x._1, y))
    override def read(
        state: Option[CC[String, A]],
        conf: Conf,
    ): Configured[CC[String, A]] = conf match {
      case Conf.Obj(List(("+", obj: Conf.Obj))) => decode(obj).map { x =>
          state.fold(x) { state =>
            val builder = factory.newBuilder
            builder ++= state
            builder ++= x
            builder.result()
          }
        }
      case obj: Conf.Obj => decode(obj)
      case _ =>
        val expect = s"Map[String, ${classTag.runtimeClass.getName}]"
        Configured.NotOk(ConfError.typeMismatch(expect, conf))
    }

    override def convert(conf: Conf): Conf = conf match {
      case Conf.Obj(List((k @ "+", v: Conf.Obj))) => Conf
          .Obj(k -> CanBuildFromDecoder.convertMap(v, ev))
      case v: Conf.Obj => CanBuildFromDecoder.convertMap(v, ev)
      case _ => conf
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

    override def convert(conf: Conf): Conf = conf match {
      case v: Conf.Lst => CanBuildFromDecoder.convertSeq(v, ev)
      case _ => conf
    }
  }

  implicit def canBuildSeq[A, C[x] <: collection.Iterable[x]](implicit
      ev: ConfDecoderEx[A],
      factory: Factory[A, C[A]],
      classTag: ClassTag[A],
  ): ConfDecoderEx[C[A]] = new ConfDecoderEx[C[A]] {
    def decode(lst: Conf.Lst) =
      buildFrom(None, lst.values, ev, factory)(identity, (_, x) => x)
    override def read(state: Option[C[A]], conf: Conf): Configured[C[A]] =
      conf match {
        case Conf.Obj(List(("+", lst: Conf.Lst))) => decode(lst).map { x =>
            state.fold(x) { state =>
              val builder = factory.newBuilder
              builder ++= state
              builder ++= x
              builder.result()
            }
          }
        case lst: Conf.Lst => decode(lst)
        case _ =>
          val expect = s"List[${classTag.runtimeClass.getName}]"
          Configured.NotOk(ConfError.typeMismatch(expect, conf))
      }

    override def convert(conf: Conf): Conf = conf match {
      case Conf.Obj(List((k @ "+", v: Conf.Lst))) => Conf
          .Obj(k -> CanBuildFromDecoder.convertSeq(v, ev))
      case v: Conf.Lst => CanBuildFromDecoder.convertSeq(v, ev)
      case _ => conf
    }
  }

  implicit final class Implicits[S, A](private val self: ConfDecoderExT[S, A])
      extends AnyVal {

    def read(state: Option[S], conf: Configured[Conf]): Configured[A] = conf
      .andThen(self.read(state, _))

    def contramap(f: Conf => Conf): ConfDecoderExT[S, A] =
      new ConfDecoderExT[S, A] {
        override def read(state: Option[S], conf: Conf): Configured[A] = self
          .read(state, f(conf))
        override def convert(conf: Conf): Conf = self.convert(f(conf))
      }

    def map[B](f: A => B): ConfDecoderExT[S, B] = new ConfDecoderExT[S, B] {
      override def read(state: Option[S], conf: Conf): Configured[B] = self
        .read(state, conf).map(f)
      override def convert(conf: Conf): Conf = self.convert(conf)
    }

    def flatMap[B](f: A => Configured[B]): ConfDecoderExT[S, B] =
      new ConfDecoderExT[S, B] {
        override def read(state: Option[S], conf: Conf): Configured[B] = self
          .read(state, conf).andThen(f)
        override def convert(conf: Conf): Conf = self.convert(conf)
      }

    def orElse(other: ConfDecoderExT[S, A]): ConfDecoderExT[S, A] =
      new ConfDecoderExT[S, A] {
        override def read(state: Option[S], conf: Conf): Configured[A] = self
          .read(state, conf).recoverWithOrCombine(other.read(state, conf))
        override def convert(conf: Conf): Conf = other.convert(self.convert(conf))
      }

    def noTypos(implicit settings: generic.Settings[A]): ConfDecoderExT[S, A] =
      NoTyposDecoder(self)

    def detectSectionRenames(implicit
        settings: generic.Settings[A],
    ): ConfDecoderExT[S, A] = SectionRenameDecoder(self)

    def withSectionRenames(
        renames: annotation.SectionRename*,
    ): ConfDecoderExT[S, A] = SectionRenameDecoder(self, renames.toList)

    def except(
        f: PartialFunction[(Option[S], Conf), Configured[A]],
    ): ConfDecoderExT[S, A] = new ConfDecoderExT[S, A] {
      override def read(state: Option[S], conf: Conf): Configured[A] = f
        .lift((state, conf)).getOrElse(self.read(state, conf))
      override def convert(conf: Conf): Conf = self.convert(conf)
    }

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

  def fromPartialConverted[A](expect: String)(fconv: PartialFunction[Conf, Conf])(
      f: PartialFunction[(Option[A], Conf), Configured[A]],
  ): ConfDecoderEx[A] = ConfDecoderExT
    .fromPartialConverted[A, A](expect)(fconv)(f)

}
