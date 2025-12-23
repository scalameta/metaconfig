package metaconfig.internal

import metaconfig.{Conf, ConfDecoder, ConfError, Configured}

import scala.annotation.tailrec
import scala.collection.mutable

object ConfGet {
  def getOrElse[A](
      some: Conf => Configured[A],
      none: => Configured[A],
  )(conf: Conf, keys: Seq[String]): Configured[A] = conf match {
    case obj: Conf.Obj => getKey(obj, keys).fold(none)(some)
    case _ => none
  }

  def getOrOK[A](
      conf: Conf,
      keys: Seq[String],
      some: Conf => Configured[A],
      none: => A,
  ): Configured[A] = getOrElse(some, Configured.ok(none))(conf, keys)

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
  ): Configured[T] = getOrOK(conf, keys, ev.read, default)

  def get[T](conf: Conf, path: String, extraNames: String*)(implicit
      ev: ConfDecoder[T],
  ): Configured[T] = conf match {
    case obj: Conf.Obj => getOrElse(ev.read, ConfError.missingField(obj, path))(
        obj,
        path +: extraNames,
      )
    case _ => ConfError.typeMismatch(s"Conf.Obj with field $path", conf, path)
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
