package metaconfig

import scala.collection.mutable
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.meta.internal.inputs._

trait MetaconfigParser {
  def fromInput(input: Input): Configured[Conf]
}

object Metaconfig {

  def getKey(obj: Conf.Obj, keys: Seq[String]): Option[Conf] =
    if (keys.isEmpty) None
    else
      obj.values
        .collectFirst { case (key, value) if key == keys.head => value }
        .orElse(getKey(obj, keys.tail))

  def get[T](conf: Conf.Obj)(default: T, path: String, extraNames: String*)(
      implicit ev: ConfDecoder[T]): Configured[T] = {
    getKey(conf, path +: extraNames) match {
      case Some(value) => ev.read(value)
      case None => Configured.Ok(default)
    }
  }
  // Copy-pasted from scala.meta inputs because it's private.
  // TODO(olafur) expose utility in inputs to get offset from line
  private[metaconfig] def getOffsetByLine(chars: Array[Char]): Array[Int] = {
    val buf = new mutable.ArrayBuffer[Int]
    buf += 0
    var i = 0
    while (i < chars.length) {
      if (chars(i) == '\n') buf += (i + 1)
      i += 1
    }
    if (buf.last != chars.length) buf += chars.length // sentinel value used for binary search
    buf.toArray
  }
}
