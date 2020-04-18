package metaconfig

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import scala.collection.mutable

sealed abstract class Input(val path: String, val text: String)
    extends Product
    with Serializable {

  def syntax: String = path

  final val chars: Array[Char] = text.toCharArray

  private val cachedLineIndices: Array[Int] = {
    val buf = new mutable.ArrayBuffer[Int]
    buf += 0
    var i = 0
    while (i < chars.length) {
      if (chars(i) == '\n')
        buf += (i + 1)
      i += 1
    }
    if (buf.last != chars.length) buf += chars.length // sentinel value used for binary search
    buf.toArray
  }

  def lineToOffset(line: Int): Int = {
    // NOTE: The length-1 part is not a typo, it's to accommodate the sentinel value.
    if (!(0 <= line && line <= cachedLineIndices.length - 1)) {
      val message =
        s"$line is not a valid line number, allowed [0..${cachedLineIndices.length - 1}]"
      throw new IllegalArgumentException(message)
    }
    cachedLineIndices(line)
  }

  def offsetToLine(offset: Int): Int = {
    val chars = this.chars
    val a = cachedLineIndices
    // NOTE: We allow chars.length, because it's a valid value for an offset.
    if (!(0 <= offset && offset <= chars.length)) {
      val message =
        s"$offset is not a valid offset, allowed [0..${chars.length}]"
      throw new IllegalArgumentException(message)
    }
    // If the file doesn't end with \n, then it's simply last_line:last_col+1.
    // But if the file does end with \n, then it's last_line+1:0.
    if (offset == chars.length && (0 < chars.length && chars(offset - 1) == '\n')) {
      return a.length - 1
    }
    var lo = 0
    var hi = a.length - 1
    while (hi - lo > 1) {
      val mid = (hi + lo) / 2
      if (offset < a(mid)) hi = mid
      else if (a(mid) == offset) return mid
      else /* if (a(mid) < offset */ lo = mid
    }
    lo
  }
}

object Input {

  case object None extends Input("<none>", "") {
    override def toString: Predef.String = "Input.None"
  }

  final case class String(override val text: Predef.String)
      extends Input("<input>", text) {
    override def toString: Predef.String =
      s"""Input.String("$text")"""
  }

  final case class VirtualFile(
      override val path: Predef.String,
      override val text: Predef.String
  ) extends Input(path, text) {
    override def toString: Predef.String =
      s"""Input.VirtualFile("$path", "...")"""
  }

  final case class File(file: Path, charset: Charset)
      extends Input(
        file.toString,
        new Predef.String(Files.readAllBytes(file), charset.name)
      )
  object File {
    def apply(file: java.io.File): Input = {
      Input.File(file.toPath, StandardCharsets.UTF_8)
    }
    def apply(path: Path): Input = {
      Input.File(path, StandardCharsets.UTF_8)
    }
  }

}
