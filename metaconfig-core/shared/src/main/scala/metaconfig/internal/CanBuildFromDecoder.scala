package metaconfig.internal

import metaconfig.{Conf, ConfConverter, ConfDecoder, ConfError, Configured}

import scala.collection.compat._
import scala.reflect.ClassTag

object CanBuildFromDecoder {

  def convertMap(conf: Conf.Obj, ev: ConfConverter): Conf.Obj = Conf
    .Obj(conf.values.map { case (k, v) => k -> ev.convert(v) })

  def map[A, CC[_, _]](implicit
      ev: ConfDecoder[A],
      factory: Factory[(String, A), CC[String, A]],
      classTag: ClassTag[A],
  ): ConfDecoder[CC[String, A]] = ConfDecoder
    .fromPartialConverted(s"Map[String, ${classTag.runtimeClass.getName}]") {
      case v: Conf.Obj => convertMap(v, ev)
    } { case Conf.Obj(values) =>
      build(values, ev, factory)(_._2, (x, y) => (x._1, y))
    }

  def convertSeq(conf: Conf.Lst, ev: ConfConverter): Conf.Lst = Conf
    .Lst(conf.values.map(ev.convert))

  def list[C[_], A](implicit
      ev: ConfDecoder[A],
      factory: Factory[A, C[A]],
      classTag: ClassTag[A],
  ): ConfDecoder[C[A]] = ConfDecoder
    .fromPartialConverted(s"List[${classTag.runtimeClass.getName}]") {
      case v: Conf.Lst => convertSeq(v, ev)
    } { case Conf.Lst(values) =>
      build(values, ev, factory)(identity, (_, x) => x)
    }

  private def build[A, B, C, Coll](
      values: List[A],
      ev: ConfDecoder[B],
      factory: Factory[C, Coll],
  )(a2conf: A => Conf, ab2c: (A, B) => C): Configured[Coll] = {
    val successB = factory.newBuilder
    val errorB = List.newBuilder[ConfError]
    successB.sizeHint(values.length)
    values.foreach { value =>
      ev.read(a2conf(value)).foreach(errorB += _)(successB += ab2c(value, _))
    }
    Configured(successB.result(), errorB.result(): _*)
  }

}
