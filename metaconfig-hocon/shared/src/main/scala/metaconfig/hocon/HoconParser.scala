// ORIGINAL LICENCE
/* Copyright 2016 UniCredit S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* Olafur Pall Geirsson has modified the original file contents
 * to better fit metaconfig needs and structure.
 */
package metaconfig
package hocon

import fastparse.all._
import fastparse.core.Parser
import metaconfig.Conf.Obj

object HoconParser {
  case class NamedFunction[T, V](f: T => V, name: String) extends (T => V) {
    def apply(t: T) = f(t)
    override def toString() = name

  }

  val isWhitespace: (Char) => Boolean = {
    // try to hit the most common ASCII ones first, then the nonbreaking
    // spaces that Java brokenly leaves out of isWhitespace.
    case ' ' | '\n' | '\u00A0' | '\u2007' | '\u202F' | '\uFEFF' /* BOM */ =>
      true;
    case c => Character.isWhitespace(c);
  }

  val isWhitespaceNoNl = (c: Char) => c != '\n' && isWhitespace(c)

  // *** Lexing ***
  //  val Whitespace = NamedFunction(isWhitespace, "Whitespace")
  val lowercase = P(CharIn('a' to 'z'))
  val uppercase = P(CharIn('A' to 'Z'))
  val letter = P(lowercase | uppercase)
  val digit = P(CharIn('0' to '9'))

  val Digits = NamedFunction('0' to '9' contains (_: Char), "Digits")
  val StringChars = NamedFunction(!"\"\\".contains(_: Char), "StringChars")
  val UnquotedStringChars =
    NamedFunction(!isWhitespaceNoNl(_: Char), "UnquotedStringChars  ")

  val keyValueSeparator = P(CharIn(":="))

  // whitespace
  //  val wspace        = P( CharsWhile(Whitespace) )
  val comment = P("#" ~ CharsWhile(_ != '\n', min = 0))
  val nlspace = P((CharsWhile(isWhitespace, min = 1) | comment).rep)
  val space = P((CharsWhile(isWhitespaceNoNl, min = 1) | comment).rep)

  val hexDigit = P(CharIn('0' to '9', 'a' to 'f', 'A' to 'F'))
  val unicodeEscape = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)
  val escape = P("\\" ~ (CharIn("\"/\\bfnrt") | unicodeEscape))

  // strings
  val strChars = P(CharsWhile(StringChars))
  val quotedString = P("\"" ~/ (strChars | escape).rep.! ~ "\"")
  val unquotedString = P(
    (letter | digit | "_" | "-" | ".")
      .rep(min = 1)
      .! // .log() // .log() // .log() // .log() // .log() // .log() // .log() // .log()
      .rep(min = 1, sep = CharsWhile(_.isSpaceChar))
      .!)
  val string = P(nlspace) ~
    P(
      quotedString |
        unquotedString |
        CharsWhile(_.isSpaceChar).!) // bit of an hack: this would parse whitespace to the end of line
      .rep(min = 1)
      .map(_.mkString.trim) // so we will trim the remaining right-side
      .map(Conf.Str)

  // *** Parsing ***
  val itemSeparator = P(("\n" ~ nlspace ~ ",".?) | ",".~/)

  val objBody = P(pair.rep(sep = itemSeparator) ~ nlspace) // .log()

  val obj: P[Seq[(String, Conf)]] = P("{" ~/ objBody ~ "}")

  val repeatedObj: P[Conf.Obj] =
    obj
      .rep(min = 1, sep = nlspace)
      .map(fields => Conf.Obj(fields.flatten.toList))

  val pair: P[(String, Conf)] = P(
    string.map(_.value) ~/ space ~
      ((keyValueSeparator ~/ jsonExpr)
        | (repeatedObj ~ space)))

  val array: P[Seq[Conf]] = P(
    "[" ~/ jsonExpr.rep(sep = itemSeparator) ~ nlspace ~ "]")

  val repeatedLst: P[Conf.Lst] =
    array
      .rep(min = 1, sep = nlspace)
      .map((arrays: Seq[Seq[Conf]]) => Conf.Lst(arrays.flatten.toList))

  val jsonExpr: P[Conf] = P(
    space ~ (repeatedObj | repeatedLst | string) ~ space) // .log()

  val root: Parser[Obj, Char, String] =
    P((&(space ~ "{") ~/ obj) | objBody ~ End).map(x => Conf.Obj(x.toList))

}
