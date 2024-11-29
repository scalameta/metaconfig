package metaconfig.internal

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured

import scala.annotation.tailrec
import scala.collection.mutable

object ConfGet {
  def getKey(conf: Conf, keys: Seq[String]): Option[Conf] = conf match {
    case obj: Conf.Obj => getKey(obj, keys)
    case _ => None
  }

  @tailrec
  private def getKey(obj: Conf.Obj, keys: Seq[String]): Option[Conf] =
    keys.headOption match {
      case Some(key) =>
        obj.values.collectFirst { case (`key`, value) => value } match {
          case x: Some[_] => x
          case _ => getKey(obj, keys.tail)
        }
      case None => None
    }

  def getOrElse[T](conf: Conf, default: T, keys: Seq[String])(implicit
      ev: ConfDecoder[T],
  ): Configured[T] = getKey(conf, keys).fold(Configured.ok(default))(ev.read)

  def get[T](conf: Conf, path: String, extraNames: String*)(implicit
      ev: ConfDecoder[T],
  ): Configured[T] = conf match {
    case obj: Conf.Obj => getKey(obj, path +: extraNames).map(ev.read)
        .getOrElse(ConfError.missingField(obj, path).notOk)
    case _ => ConfError.typeMismatch(s"Conf.Obj with field $path", conf, path)
        .notOk
  }

  def getNested[T](conf: Conf, keys: String*)(implicit
      ev: ConfDecoder[T],
  ): Configured[T] = conf.getNestedConf(keys: _*).andThen(ev.read)

  // Copy-pasted from scala.meta inputs because it's private.
  // TODO(olafur) expose utility in inputs to get offset from line
  private[metaconfig] def getOffsetByLine(chars: Array[Char]): Array[Int] = {
    val buf = new mutable.ArrayBuffer[Int]
    buf += 0
    var i = 0
    while (i < chars.length) {
      if (chars(i) == '\n') buf += i + 1
      i += 1
    }
    if (buf.last != chars.length) buf += chars.length // sentinel value used for binary search
    buf.toArray
  }

}
