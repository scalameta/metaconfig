package metaconfig

import metaconfig.internal.ConfGet

@deprecated(
  "Moved to metaconfig.internal.ConfGet. Use Conf.get* variants instead.",
  "0.6")
object Metaconfig {

  def getKey(obj: Conf, keys: Seq[String]): Option[Conf] =
    ConfGet.getKey(obj, keys)

  def getOrElse[T](conf: Conf, default: T, path: String, extraNames: String*)(
      implicit ev: ConfDecoder[T]): Configured[T] =
    ConfGet.getOrElse[T](conf, default, path)

  def get[T](conf: Conf, path: String, extraNames: String*)(
      implicit ev: ConfDecoder[T]): Configured[T] =
    ConfGet.get[T](conf, path, extraNames: _*)

}
