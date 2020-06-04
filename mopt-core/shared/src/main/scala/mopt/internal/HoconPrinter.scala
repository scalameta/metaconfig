package mopt.internal

import mopt.Conf
import mopt.ConfEncoder
import org.typelevel.paiges.Doc._
import org.typelevel.paiges.Doc

object HoconPrinter {

  def toHocon[T: ConfEncoder](value: T): Doc = {
    toHocon(ConfEncoder[T].write(value))
  }

  def toHocon(conf: Conf): Doc = {
    def loop(c: Conf): Doc = {
      c match {
        case Conf.Null() => text("null")
        case Conf.Num(num) => str(num)
        case Conf.Str(str) => quoteString(str)
        case Conf.Bool(bool) => str(bool)
        case Conf.Lst(lst) =>
          if (lst.isEmpty) text("[]")
          else {
            val parts = intercalate(line, lst.map {
              case c: Conf.Obj =>
                wrap('{', '}', loop(c))
              case x => loop(x)
            })
            wrap('[', ']', parts)
          }
        case Conf.Obj(obj) =>
          intercalate(line, obj.map {
            case (k, v) =>
              text(k) + text(" = ") + loop(v)
          })
      }
    }

    loop(flatten(conf))
  }

  def flatten(c: Conf): Conf = c match {
    case Conf.Obj(obj) =>
      val flattened = obj.map {
        case (k, v) => (k, flatten(v))
      }
      val next = flattened.flatMap {
        case (key, Conf.Obj(nested)) =>
          nested.map {
            case (k, v) => s"${quote(key)}.$k" -> v
          }
        case (key, value) => (quote(key), value) :: Nil
      }
      Conf.Obj(next)
    case Conf.Lst(lst) =>
      Conf.Lst(lst.map(flatten))
    case x => x
  }

  private def quote(key: String): String =
    if (key.indexOf('.') < 0) key
    else "\"" + key + "\""

  private val quote = char('"')

  // Spec is here:
  // https://github.com/lightbend/config/blob/master/HOCON.md#unquoted-strings
  // but this method is conservative and quotes if the string contains non-letter characters
  private def needsQuote(str: String): Boolean =
    str.isEmpty ||
      str.startsWith("true") ||
      str.startsWith("false") ||
      str.startsWith("null") ||
      str.exists(!_.isLetter)

  private def quoteString(str: String): Doc =
    if (needsQuote(str)) quote + text(str) + quote
    else text(str)

  private def wrap(open: Char, close: Char, doc: Doc): Doc = {
    (char(open) + line + doc).nested(2) + line + char(close)
  }

}
