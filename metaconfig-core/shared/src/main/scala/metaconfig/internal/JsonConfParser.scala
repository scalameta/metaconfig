package metaconfig.internal

import java.nio.CharBuffer
import metaconfig.Input
import metaconfig.Position
import ujson._

final class JsonConfParser[J](input: Input)
    extends SyncParser[J]
    with CharBasedParser[J] {
  var line = 0
  val chars = input.chars
  val wrapped = CharBuffer.wrap(chars)

  override def die(i: Int, msg: String): Nothing = {
    val pos = Position.Range(input, i, i)
    val error = pos.pretty("error", msg)
    throw ParseException(error, i, pos.startLine, pos.startColumn)
  }

  def column(i: Int): Int = i
  def newline(i: Int): Unit = { line += 1 }
  def reset(i: Int): Int = {
    if (atEof(i + 2)) {
      i
    } else {
      at(i) match {
        case '/' => // comment?
          at(i + 1) match {
            case '/' =>
              var curr = i + 2
              // TODO: handle \r\n
              while (!atEof(curr) && at(curr) != '\n') {
                curr += 1
              }
              curr
            case _ =>
              i
          }
        case ',' => // trailing comma?
          at(i + 1) match {
            case '\n' =>
              var curr = i + 2
              // TODO handle comments
              while (!atEof(curr) && Character.isWhitespace(at(curr))) {
                curr += 1
              }
              at(curr) match {
                case ']' | '}' =>
                  curr
                case _ =>
                  i
              }
            case _ =>
              i
          }
        case _ =>
          i
      }
    }
  }
  def checkpoint(state: Int, i: Int, stack: List[ObjArrVisitor[_, J]]): Unit =
    ()
  def at(i: Int): Char = {
    if (i >= chars.length) throw new StringIndexOutOfBoundsException(i)
    chars(i)
  }
  def at(i: Int, j: Int): CharSequence = wrapped.subSequence(i, j)
  def atEof(i: Int): Boolean = i >= chars.length
  def close(): Unit = ()
}

object JsonConfParser extends Transformer[String] {
  def transform[T](j: String, f: Visitor[_, T]) =
    new JsonConfParser(Input.String(j)).parse(f)
}
