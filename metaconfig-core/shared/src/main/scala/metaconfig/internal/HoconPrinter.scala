package metaconfig.internal

import metaconfig.Conf
import metaconfig.ConfEncoder

import org.typelevel.paiges.Doc
import org.typelevel.paiges.Doc._

object HoconPrinter {

  def toHocon[T: ConfEncoder](value: T): Doc =
    toHocon(ConfEncoder[T].write(value))

  def toHocon(conf: Conf): Doc = {
    def loop(c: Conf): Doc = c match {
      case Conf.Null() | Conf.Str(null) => text("null")
      case Conf.Num(num) => str(num)
      case Conf.Str(str) => quoteString(str)
      case Conf.Bool(bool) => str(bool)
      case Conf.Lst(Nil) => text("[]")
      case Conf.Lst(lst) =>
        val elems = lst.map {
          case c: Conf.Obj => wrap('{', '}', loop(c))
          case x => loop(x)
        }
        wrap('[', ']', intercalate(line, elems))
      case Conf.Obj(obj) =>
        val elems = obj.map { case (k, v) => text(k) + text(" = ") + loop(v) }
        intercalate(line, elems)
    }

    loop(flatten(conf))
  }

  def flatten(c: Conf): Conf = c match {
    case Conf.Obj(obj) =>
      val flattened = obj.map { case (k, v) => (k, flatten(v)) }
      val next = flattened.flatMap {
        case (key, Conf.Obj(nested)) => nested.map { case (k, v) =>
            s"${quote(key)}.$k" -> v
          }
        case (key, value) => (quote(key), value) :: Nil
      }
      Conf.Obj(next)
    case Conf.Lst(lst) => Conf.Lst(lst.map(flatten))
    case x => x
  }

  private def quote(key: String): String =
    if (key.indexOf('.') < 0) key else "\"" + key + "\""

  private val quote = char('"')

  // Spec is here:
  // https://github.com/lightbend/config/blob/main/HOCON.md#unquoted-strings
  // but this method is conservative and quotes if the string contains non-letter characters
  private def needsQuote(str: String): Boolean = str.isEmpty ||
    str.startsWith("true") || str.startsWith("false") ||
    str.startsWith("null") || str.exists(!_.isLetter)

  private def quoteString(str: String): Doc =
    if (needsQuote(str)) quote + text(str) + quote else text(str)

  private def wrap(open: Char, close: Char, doc: Doc): Doc =
    (char(open) + line + doc).nested(2) + line + char(close)

}
