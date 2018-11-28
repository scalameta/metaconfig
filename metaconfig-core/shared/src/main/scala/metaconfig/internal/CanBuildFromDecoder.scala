package metaconfig.internal

import scala.language.higherKinds

import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok

object CanBuildFromDecoder {
  def map[A](
      implicit ev: ConfDecoder[A],
      classTag: ClassTag[A]
  ): ConfDecoder[Map[String, A]] =
    ConfDecoder.instanceExpect[Map[String, A]](
      s"Map[String, ${classTag.runtimeClass.getName}]"
    ) {
      case Conf.Obj(values) =>
        val results = values.map {
          case (key, value) => ev.read(value).map(key -> _)
        }
        ConfError.fromResults(results) match {
          case Some(err) => NotOk(err)
          case None =>
            Ok(results.collect { case Ok(x) => x }.toMap)
        }
    }
  def list[C[_], A](
      implicit ev: ConfDecoder[A],
      cbf: CanBuildFrom[Nothing, A, C[A]],
      classTag: ClassTag[A]
  ): ConfDecoder[C[A]] =
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

}
