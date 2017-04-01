package metaconfig

import scala.reflect.ClassTag

object Metaconfig {
  def getKey(obj: Conf.Obj, keys: Seq[String]): Option[Conf] =
    if (keys.isEmpty) None
    else
      obj.values
        .collectFirst { case (key, value) if key == keys.head => value }
        .orElse(getKey(obj, keys.tail))

  def get[T](conf: Conf.Obj)(default: T, path: String, extraNames: String*)(
      implicit ev: ConfDecoder[T],
      clazz: ClassTag[T]): T = {
    getKey(conf, path +: extraNames) match {
      case Some(value) =>
        ev.read(value) match {
          case Right(e) => e
          case Left(e: java.lang.IllegalArgumentException) =>
            val simpleName = clazz.runtimeClass.getSimpleName
            val msg =
              s"Error reading field '$path'. " +
                s"Expected argument of type $simpleName. " +
                s"Obtained ${e.getMessage}"
            throw _root_.metaconfig.ConfigError(msg)
          case Left(e) => throw e
        }
      case None => default
    }
  }
}
