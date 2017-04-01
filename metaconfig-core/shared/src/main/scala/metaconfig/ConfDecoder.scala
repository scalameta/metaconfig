package metaconfig

import scala.reflect.ClassTag

trait HasFields {
  def fields: Map[String, Any]
}

trait ConfDecoder[T] { self =>
  // NOTE. This signature is a mess. It should be `read(conf: Conf): Result[T]`.
  def read(any: Conf): Result[T]

  def map[TT](f: T => TT): ConfDecoder[TT] = self.flatMap(x => Right(f(x)))

  def flatMap[TT](f: T => Result[TT]): ConfDecoder[TT] =
    new ConfDecoder[TT] {
      override def read(any: Conf): Result[TT] = self.read(any) match {
        case Right(x) => f(x)
        case Left(x) => Left(x)
      }
    }
}

object ConfDecoder {

  def fail[T: ClassTag](x: Conf): Result[T] = {
    Left(new IllegalArgumentException(s"value '${x.show}' of type ${x.kind}."))
  }

  def instance[T](f: PartialFunction[Conf, Result[T]])(
      implicit ev: ClassTag[T]) =
    new ConfDecoder[T] {
      override def read(any: Conf): Result[T] = {
        f.applyOrElse(any, (x: Conf) => fail[T](x))
      }
    }
  implicit val intR: ConfDecoder[Int] = instance[Int] {
    case Conf.Num(x) => Right(x.toInt)
  }
  implicit val stringR: ConfDecoder[String] = instance[String] {
    case Conf.Str(x) => Right(x)
  }
  implicit val boolR: ConfDecoder[Boolean] = instance[scala.Boolean] {
    case Conf.Bool(x) => Right(x)
  }

  implicit def seqR[T](implicit ev: ConfDecoder[T]): ConfDecoder[Seq[T]] =
    instance[Seq[T]] {
      case Conf.Lst(lst) =>
        val res = lst.map(ev.read)
        val lefts = res.collect { case Left(e) => e }
        if (lefts.nonEmpty) Left(ConfigErrors(lefts))
        else Right(res.collect { case Right(e) => e })
    }

  implicit def setR[T](implicit ev: ConfDecoder[T]): ConfDecoder[Set[T]] =
    instance[Set[T]] {
      case e => seqR[T].read(e).right.map(_.toSet)
    }

  // TODO(olafur) generic can build from reader
  implicit def mapR[V](
      implicit evV: ConfDecoder[V]): ConfDecoder[Map[String, V]] =
    instance[Map[String, V]] {
      case Conf.Obj(values) =>
        val res = values.map {
          case (k, v) =>
            k -> evV.read(v)
        }
        val lefts: Seq[Throwable] = res.collect {
          case (_, Left(e)) => e
        }
        if (lefts.nonEmpty) Left(ConfigErrors(lefts))
        else {
          val resultMap = res.collect { case (key, Right(e)) => key -> e }
          Right(resultMap.toMap)
        }
    }
}
