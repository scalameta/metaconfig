/* MIT License

Copyright (c) 2019 Li Haoyi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. */

package metaconfig.pprint

/**
  * Summoning an implicit `TPrint[T]` provides a pretty-printed
  * string representation of the type `T`, much better than is
  * provided by the default `Type#toString`. In particular
  *
  * - More forms are properly supported and printed
  * - Prefixed Types are printed un-qualified, according to
  *   what's currently in scope
  */
trait TPrint[T] {
  def render(implicit tpc: TPrintColors): fansi.Str

}

object TPrint extends TPrintLowPri {
  def recolor[T](s: fansi.Str): TPrint[T] = {
    new TPrint[T] {
      def render(implicit tpc: TPrintColors) = {
        val colors = s.getColors
        val updatedColors = colors.map { c =>
          if (c == fansi.Color.Green.applyMask) tpc.typeColor.applyMask else 0L
        }
        fansi.Str.fromArrays(s.getChars, updatedColors)
      }
    }
  }
  def implicitly[T](implicit t: TPrint[T]): TPrint[T] = t
  implicit val NothingTPrint: TPrint[Nothing] =
    recolor[Nothing](fansi.Color.Green("Nothing"))

}

case class TPrintColors(typeColor: fansi.Attrs)

object TPrintColors {
  implicit object BlackWhite extends TPrintColors(fansi.Attrs())
  object Colors extends TPrintColors(fansi.Color.Green) {
    implicit val Colored: TPrintColors = this
  }
}
