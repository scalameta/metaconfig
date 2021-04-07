package metaconfig.internal

import metaconfig.Input
import metaconfig.Position
import ujson._
import upickle.core.Abort
import upickle.core.AbortException
import upickle.core.BufferingCharParser
import upickle.core.ObjArrVisitor
import upickle.core.ObjVisitor
import upickle.core.Visitor

import java.nio.CharBuffer
import scala.annotation.switch
import scala.annotation.tailrec

/**
  * Code mostly copied (except metaconfig specific parts)
  * from ujson.CharParser in ujson:1.3.8
  */
final class JsonConfParser[J](input: Input) extends BufferingCharParser {

  // metaconfig specific
  var line = 0
  val chars = input.chars
  val wrapped: CharBuffer = CharBuffer.wrap(chars)

  private[this] val sLength = input.text.length()
  override def growBuffer(until: Int): Unit = ()
  def readDataIntoBuffer(
      buffer: Array[Char],
      bufferOffset: Int
  ): (Array[Char], Boolean, Int) = {
    if (buffer == null) (input.chars, sLength == 0, sLength)
    else (buffer, true, -1)
  }

  def die(i: Int, msg: String): Nothing = {
    val pos = Position.Range(input, i, i)
    val error = pos.pretty("error", msg)
    throw new ParseException(error, i) {
      // super.getMessage appends useless "at index N" suffix.
      override def getMessage: String = error
    }
  }

  // metaconfig specific end

  private[this] val elemOps = upickle.core.CharOps
  private[this] val outputBuilder = new upickle.core.CharBuilder()

  def requestUntilOrThrow(i: Int): Unit = {
    if (requestUntil(i)) throw new IncompleteParseException("exhausted input")
  }
  override def getCharSafe(i: Int): Char = {
    requestUntilOrThrow(i)
    getCharUnsafe(i)
  }

  /**
    * Return true iff 'i' is at or beyond the end of the input (EOF).
    */
  protected[this] def atEof(i: Int): Boolean = requestUntil(i)

  /**
    * Should be called when parsing is finished.
    */
  protected[this] def close(): Unit = ()

  /**
    * Valid parser states.
    */
  @inline private[this] final val ARRBEG = 6
  @inline private[this] final val OBJBEG = 7
  @inline private[this] final val DATA = 1
  @inline private[this] final val KEY = 2
  @inline private[this] final val COLON = 3
  @inline private[this] final val ARREND = 4
  @inline private[this] final val OBJEND = 5

  /**
    * Parse the JSON document into a single JSON value.
    *
    * The parser considers documents like '333', 'true', and '"foo"' to be
    * valid, as well as more traditional documents like [1,2,3,4,5]. However,
    * multiple top-level objects are not allowed.
    */
  final def parse(facade: Visitor[_, J]): J = {
    val (value, i) = parseTopLevel(0, facade)
    var j = i
    while (!atEof(j)) {
      (getCharSafe(j): @switch) match {
        case '\n' | ' ' | '\t' | '\r' => j += 1
        case _ => die(j, "expected whitespace or eof")
      }
    }
    if (!atEof(j)) die(j, "expected eof")
    close()
    value
  }

  /**
    * Parse the given number, and add it to the given context.
    *
    * We don't actually instantiate a number here, but rather pass the
    * string of for future use. Facades can choose to be lazy and just
    * store the string. This ends up being way faster and has the nice
    * side-effect that we know exactly how the user represented the
    * number.
    */
  protected[this] final def parseNum(
      i: Int,
      ctxt: ObjArrVisitor[Any, J],
      facade: Visitor[_, J]
  ): Int = {
    var j = i
    var c = getCharSafe(j)
    var decIndex = -1
    var expIndex = -1

    if (c == '-') {
      j += 1
      c = getCharSafe(j)
    }
    if (c == '0') {
      j += 1
      c = getCharSafe(j)
    } else {
      val j0 = j
      while (elemOps.within('0', c, '9')) {
        j += 1;
        c = getCharSafe(j)
      }
      if (j == j0) die(i, "expected digit")
    }

    if (c == '.') {
      decIndex = j - i
      j += 1
      c = getCharSafe(j)
      val j0 = j
      while (elemOps.within('0', c, '9')) {
        j += 1
        c = getCharSafe(j)
      }
      if (j0 == j) die(i, "expected digit")
    }

    if (c == 'e' || c == 'E') {
      expIndex = j - i
      j += 1
      c = getCharSafe(j)
      if (c == '+' || c == '-') {
        j += 1
        c = getCharSafe(j)
      }
      val j0 = j
      while (elemOps.within('0', c, '9')) {
        j += 1
        c = getCharSafe(j)
      }
      if (j0 == j) die(i, "expected digit")
    }

    ctxt.visitValue(
      visitFloat64StringPartsWithWrapper(facade, decIndex, expIndex, i, j),
      i
    )
    j
  }

  def visitFloat64StringPartsWithWrapper(
      facade: Visitor[_, J],
      decIndex: Int,
      expIndex: Int,
      i: Int,
      j: Int
  ): J = {
    facade.visitFloat64StringParts(
      unsafeCharSeqForRange(i, j - i),
      decIndex,
      expIndex,
      i
    )
  }

  /**
    * Parse the given number, and add it to the given context.
    *
    * This method is a bit slower than parseNum() because it has to be
    * sure it doesn't run off the end of the input.
    *
    * Normally (when operating in rparse in the context of an outer
    * array or object) we don't need to worry about this and can just
    * grab characters, because if we run out of characters that would
    * indicate bad input. This is for cases where the number could
    * possibly be followed by a valid EOF.
    *
    * This method has all the same caveats as the previous method.
    */
  protected[this] final def parseNumTopLevel(
      i: Int,
      facade: Visitor[_, J]
  ): (J, Int) = {
    var j = i
    var c = getCharSafe(j)
    var decIndex = -1
    var expIndex = -1

    if (c == '-') {
      // any valid input will require at least one digit after -
      j += 1
      c = getCharSafe(j)
    }
    if (c == '0') {
      j += 1
      if (atEof(j)) {
        return (
          visitFloat64StringPartsWithWrapper(facade, decIndex, expIndex, i, j),
          j
        )
      }
      c = getCharSafe(j)
    } else {
      val j0 = j
      while (elemOps.within('0', c, '9')) {
        j += 1
        if (atEof(j)) {
          return (
            visitFloat64StringPartsWithWrapper(
              facade,
              decIndex,
              expIndex,
              i,
              j
            ),
            j
          )
        }
        c = getCharSafe(j)
      }
      if (j0 == j) die(i, "expected digit")
    }

    if (c == '.') {
      // any valid input will require at least one digit after .
      decIndex = j - i
      j += 1
      c = getCharSafe(j)
      val j0 = j
      while (elemOps.within('0', c, '9')) {
        j += 1
        if (atEof(j)) {
          return (
            visitFloat64StringPartsWithWrapper(
              facade,
              decIndex,
              expIndex,
              i,
              j
            ),
            j
          )
        }
        c = getCharSafe(j)
      }
      if (j0 == j) die(i, "expected digit")
    }

    if (c == 'e' || c == 'E') {
      // any valid input will require at least one digit after e, e+, etc
      expIndex = j - i
      j += 1
      c = getCharSafe(j)
      if (c == '+' || c == '-') {
        j += 1
        c = getCharSafe(j)
      }
      val j0 = j
      while (elemOps.within('0', c, '9')) {
        j += 1
        if (atEof(j)) {
          return (
            visitFloat64StringPartsWithWrapper(
              facade,
              decIndex,
              expIndex,
              i,
              j
            ),
            j
          )
        }
        c = getCharSafe(j)
      }
      if (j0 == j) die(i, "expected digit")
    }

    (visitFloat64StringPartsWithWrapper(facade, decIndex, expIndex, i, j), j)
  }

  /**
    * Generate a Char from the hex digits of "\u1234" (i.e. "1234").
    *
    * NOTE: This is only capable of generating characters from the basic plane.
    * This is why it can only return Char instead of Int.
    */
  protected[this] final def descape(i: Int): Char = {
    import upickle.core.RenderUtils.hex
    var x = 0
    x = (x << 4) | hex(getCharSafe(i + 2).toInt)
    x = (x << 4) | hex(getCharSafe(i + 3).toInt)
    x = (x << 4) | hex(getCharSafe(i + 4).toInt)
    x = (x << 4) | hex(getCharSafe(i + 5).toInt)
    x.toChar
  }

  /**
    * Parse the JSON constant "true".
    *
    * Note that this method assumes that the first character has already been checked.
    */
  protected[this] final def parseTrue(i: Int, facade: Visitor[_, J]): J = {
    requestUntilOrThrow(i + 3)
    if (getCharUnsafe(i + 1) == 'r' && getCharUnsafe(i + 2) == 'u' && getCharUnsafe(
        i + 3
      ) == 'e') {
      facade.visitTrue(i)
    } else {
      die(i, "expected true")
    }
  }

  /**
    * Parse the JSON constant "false".
    *
    * Note that this method assumes that the first character has already been checked.
    */
  protected[this] final def parseFalse(i: Int, facade: Visitor[_, J]): J = {
    requestUntilOrThrow(i + 4)

    if (getCharUnsafe(i + 1) == 'a' && getCharUnsafe(i + 2) == 'l' && getCharUnsafe(
        i + 3
      ) == 's' && getCharUnsafe(i + 4) == 'e') {
      facade.visitFalse(i)
    } else {
      die(i, "expected false")
    }
  }

  /**
    * Parse the JSON constant "null".
    *
    * Note that this method assumes that the first character has already been checked.
    */
  protected[this] final def parseNull(i: Int, facade: Visitor[_, J]): J = {
    requestUntilOrThrow(i + 3)
    if (getCharUnsafe(i + 1) == 'u' && getCharUnsafe(i + 2) == 'l' && getCharUnsafe(
        i + 3
      ) == 'l') {
      facade.visitNull(i)
    } else {
      die(i, "expected null")
    }
  }

  protected[this] final def parseTopLevel(
      i: Int,
      facade: Visitor[_, J]
  ): (J, Int) = {
    try parseTopLevel0(i, facade)
    catch reject(i)
  }

  /**
    * Parse and return the next JSON value and the position beyond it.
    */
  @tailrec
  protected[this] final def parseTopLevel0(
      i: Int,
      facade: Visitor[_, J]
  ): (J, Int) = {
    (getCharSafe(i): @switch) match {
      // ignore whitespace
      case ' ' | '\t' | 'r' => parseTopLevel0(i + 1, facade)
      case '\n' => parseTopLevel0(i + 1, facade)

      // if we have a recursive top-level structure, we'll delegate the parsing
      // duties to our good friend rparse().
      case '[' => parseNested(ARRBEG, i + 1, facade.visitArray(-1, i), Nil)
      case '{' => parseNested(OBJBEG, i + 1, facade.visitObject(-1, i), Nil)

      // we have a single top-level number
      case '-' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
        parseNumTopLevel(i, facade)

      // we have a single top-level string
      case '"' => parseStringTopLevel(i, facade)

      // we have a single top-level constant
      case 't' => (parseTrue(i, facade), i + 4)
      case 'f' => (parseFalse(i, facade), i + 5)
      case 'n' => (parseNull(i, facade), i + 4)

      // invalid
      case _ => die(i, "expected json value")
    }
  }

  def reject(j: Int): PartialFunction[Throwable, Nothing] = {
    case e: Abort =>
      throw new AbortException(e.msg, j, -1, -1, e)
  }

  /**
    * Tail-recursive parsing method to do the bulk of JSON parsing.
    *
    * This single method manages parser states, data, etc. Except for
    * parsing non-recursive values (like strings, numbers, and
    * constants) all important work happens in this loop (or in methods
    * it calls, like reset()).
    *
    * Currently the code is optimized to make use of switch
    * statements. Future work should consider whether this is better or
    * worse than manually constructed if/else statements or something
    * else. Also, it may be possible to reorder some cases for speed
    * improvements.
    *
    * @param j index/position in the source json
    * @param path the json path in the tree
    */
  @tailrec
  protected[this] final def parseNested(
      state: Int,
      i: Int,
      stackHead: ObjArrVisitor[_, J],
      stackTail: List[ObjArrVisitor[_, J]]
  ): (J, Int) = {
    (getCharSafe(i): @switch) match {
      case ' ' | '\t' | '\r' | '\n' =>
        parseNested(state, i + 1, stackHead, stackTail)

      case '"' =>
        state match {
          case KEY | OBJBEG =>
            val nextJ =
              try parseStringKey(i, stackHead)
              catch reject(i)
            parseNested(COLON, nextJ, stackHead, stackTail)

          case DATA | ARRBEG =>
            val nextJ =
              try parseStringValue(i, stackHead)
              catch reject(i)
            parseNested(
              collectionEndFor(stackHead),
              nextJ,
              stackHead,
              stackTail
            )

          case _ => dieWithFailureMessage(i, state)
        }

      case ':' =>
        // we are in an object just after a key, expecting to see a colon.
        state match {
          case COLON => parseNested(DATA, i + 1, stackHead, stackTail)
          case _ => dieWithFailureMessage(i, state)
        }

      case '[' =>
        failIfNotData(state, i)
        val ctx =
          try stackHead.subVisitor.asInstanceOf[Visitor[_, J]].visitArray(-1, i)
          catch reject(i)
        parseNested(ARRBEG, i + 1, ctx, stackHead :: stackTail)

      case '{' =>
        failIfNotData(state, i)
        val ctx =
          try stackHead.subVisitor
            .asInstanceOf[Visitor[_, J]]
            .visitObject(-1, i)
          catch reject(i)
        parseNested(OBJBEG, i + 1, ctx, stackHead :: stackTail)

      case '-' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
        failIfNotData(state, i)
        val ctx =
          try parseNum(
            i,
            stackHead.narrow,
            stackHead.subVisitor.asInstanceOf[Visitor[_, J]]
          )
          catch reject(i)
        parseNested(collectionEndFor(stackHead), ctx, stackHead, stackTail)

      case 't' =>
        failIfNotData(state, i)
        try stackHead.narrow.visitValue(
          parseTrue(i, stackHead.subVisitor.asInstanceOf[Visitor[_, J]]),
          i
        )
        catch reject(i)
        parseNested(collectionEndFor(stackHead), i + 4, stackHead, stackTail)

      case 'f' =>
        failIfNotData(state, i)
        try stackHead.narrow.visitValue(
          parseFalse(i, stackHead.subVisitor.asInstanceOf[Visitor[_, J]]),
          i
        )
        catch reject(i)
        parseNested(collectionEndFor(stackHead), i + 5, stackHead, stackTail)

      case 'n' =>
        failIfNotData(state, i)
        try stackHead.narrow.visitValue(
          parseNull(i, stackHead.subVisitor.asInstanceOf[Visitor[_, J]]),
          i
        )
        catch reject(i)
        parseNested(collectionEndFor(stackHead), i + 4, stackHead, stackTail)

      case ',' =>
        // metaconfig specific start
        val next = trailingComma(i)
        if (next != i)
          parseNested(state, next, stackHead, stackTail)
        else {
          dropBufferUntil(next)
          (state: @switch) match {
            case ARREND => parseNested(DATA, next + 1, stackHead, stackTail)
            case OBJEND => parseNested(KEY, next + 1, stackHead, stackTail)
            case _ => dieWithFailureMessage(next, state)
          }
        }
      // metaconfig specific end
      case ']' =>
        (state: @switch) match {
          case ARREND | ARRBEG =>
            tryCloseCollection(stackHead, stackTail, i) match {
              case Some(t) => t
              case None =>
                val stackTailHead = stackTail.head
                parseNested(
                  collectionEndFor(stackTailHead),
                  i + 1,
                  stackTailHead,
                  stackTail.tail
                )
            }
          case _ => dieWithFailureMessage(i, state)
        }

      case '}' =>
        (state: @switch) match {
          case OBJEND | OBJBEG =>
            tryCloseCollection(stackHead, stackTail, i) match {
              case Some(t) => t
              case None =>
                val stackTailHead = stackTail.head
                parseNested(
                  collectionEndFor(stackTailHead),
                  i + 1,
                  stackTailHead,
                  stackTail.tail
                )
            }
          case _ => dieWithFailureMessage(i, state)
        }
      // metaconfig specific start
      case '/' =>
        val next = comment(i)
        parseNested(state, next, stackHead, stackTail)
      // metaconfig specific end
      case _ => dieWithFailureMessage(i, state)

    }
  }

  def dieWithFailureMessage(i: Int, state: Int): Nothing = {
    val expected = state match {
      case ARRBEG => "json value or ]"
      case OBJBEG => "json value or }"
      case DATA => "json value"
      case KEY => "json string key"
      case COLON => ":"
      case ARREND => ", or ]"
      case OBJEND => ", or }"
    }
    die(i, s"expected $expected")
  }

  def failIfNotData(state: Int, i: Int): Unit = (state: @switch) match {
    case DATA | ARRBEG => // do nothing
    case _ => dieWithFailureMessage(i, state)
  }

  def tryCloseCollection(
      stackHead: ObjArrVisitor[_, J],
      stackTail: List[ObjArrVisitor[_, J]],
      i: Int
  ): Option[(J, Int)] = {
    if (stackTail.isEmpty) {
      Some(
        try stackHead.visitEnd(i)
        catch reject(i),
        i + 1
      )
    } else {
      val ctxt2 = stackTail.head.narrow
      try ctxt2.visitValue(stackHead.visitEnd(i), i)
      catch reject(i)
      None

    }
  }
  def collectionEndFor(stackHead: ObjArrVisitor[_, _]): Int = {
    if (stackHead.isObj) OBJEND
    else ARREND
  }

  /**
    * See if the string has any escape sequences. If not, return the
    * end of the string. If so, bail out and return -1.
    *
    * This method expects the data to be in UTF-16 and accesses it as
    * chars.
    */
  protected[this] final def parseStringSimple(i: Int): Int = {
    var j = i
    var c = elemOps.toUnsignedInt(getCharSafe(j))
    while (c != '"') {
      if (c < ' ') die(j, s"control char (${c}) in string")
      if (c == '\\' || c > 127) return -1 - j
      j += 1
      c = elemOps.toUnsignedInt(getCharSafe(j))
    }
    j + 1
  }

  /**
    * Parse a string that is known to have escape sequences.
    */
  protected[this] final def parseStringComplex(i0: Int): Int = {
    var i = i0
    var c = elemOps.toUnsignedInt(getCharSafe(i))
    while (c != '"') {

      if (c < ' ') die(i, s"control char (${c}) in string")
      else if (c == '\\') {
        (getCharSafe(i + 1): @switch) match {
          case 'b' => { outputBuilder.append('\b'); i += 2 }
          case 'f' => { outputBuilder.append('\f'); i += 2 }
          case 'n' => { outputBuilder.append('\n'); i += 2 }
          case 'r' => { outputBuilder.append('\r'); i += 2 }
          case 't' => { outputBuilder.append('\t'); i += 2 }

          case '"' => { outputBuilder.append('"'); i += 2 }
          case '/' => { outputBuilder.append('/'); i += 2 }
          case '\\' => { outputBuilder.append('\\'); i += 2 }

          // if there's a problem then descape will explode
          case 'u' =>
            val d = descape(i)
            outputBuilder.appendC(d)

            i += 6

          case c => die(i + 1, s"illegal escape sequence after \\")
        }
      } else {
        // this case is for "normal" code points that are just one Char.
        //
        // we don't have to worry about surrogate pairs, since those
        // will all be in the ranges D800–DBFF (high surrogates) or
        // DC00–DFFF (low surrogates).
        outputBuilder.append(c)
        i += 1
      }
      c = elemOps.toUnsignedInt(getCharSafe(i))
    }

    i + 1
  }

  /**
    * Parse the string according to JSON rules, and add to the given
    * context.
    *
    * This method expects the data to be in UTF-16, and access it as
    * Char. It performs the correct checks to make sure that we don't
    * interpret a multi-char code point incorrectly.
    */
  protected[this] final def parseStringValue(
      i: Int,
      stackHead: ObjArrVisitor[_, J]
  ): Int = {

    val k = parseStringSimple(i + 1)
    if (k >= 0) {
      visitString(i, unsafeCharSeqForRange(i + 1, k - i - 2), stackHead)
      k
    } else {
      val k2 = parseStringToOutputBuilder(i, k)
      visitString(i, outputBuilder.makeString(), stackHead)
      k2
    }
  }

  protected[this] final def parseStringKey(
      i: Int,
      stackHead: ObjArrVisitor[_, J]
  ): Int = {

    val k = parseStringSimple(i + 1)
    if (k >= 0) {
      visitStringKey(i, unsafeCharSeqForRange(i + 1, k - i - 2), stackHead)
      k
    } else {
      val k2 = parseStringToOutputBuilder(i, k)
      visitStringKey(i, outputBuilder.makeString(), stackHead)
      k2
    }
  }

  def parseStringToOutputBuilder(i: Int, k: Int): Int = {
    outputBuilder.reset()
    appendCharsToBuilder(outputBuilder, i + 1, -k - 2 - i)
    val k2 = parseStringComplex(-k - 1)
    k2
  }

  def visitString(
      i: Int,
      s: CharSequence,
      stackHead: ObjArrVisitor[_, J]
  ): Unit = {
    val v = stackHead.subVisitor.visitString(s, i)
    stackHead.narrow.visitValue(v, i)
  }
  def visitStringKey(
      i: Int,
      s: CharSequence,
      stackHead: ObjArrVisitor[_, J]
  ): Unit = {
    val obj = stackHead.asInstanceOf[ObjVisitor[Any, _]]
    val keyVisitor = obj.visitKey(i)
    obj.visitKeyValue(keyVisitor.visitString(s, i))
  }

  protected[this] final def parseStringTopLevel(
      i: Int,
      facade: Visitor[_, J]
  ): (J, Int) = {

    val k = parseStringSimple(i + 1)
    if (k >= 0) {
      val res = facade.visitString(unsafeCharSeqForRange(i + 1, k - i - 2), i)
      (res, k)
    } else {
      val k2 = parseStringToOutputBuilder(i, k)
      val res = facade.visitString(outputBuilder.makeString(), i)
      (res, k2)
    }
  }

  // metaconfig specific
  private def trailingComma(i: Int): Int = getCharSafe(i) match {
    case ',' =>
      var curr = i + 1
      var done = false
      while (!atEof(curr) && !done) {
        getCharSafe(curr) match {
          case '/' =>
            curr = comment(curr) + 1
          case ' ' | '\n' =>
            curr = curr + 1
          case _ =>
            done = true
        }
      }
      getCharSafe(curr) match {
        case ']' | '}' =>
          curr
        case _ =>
          i
      }
    case _ => i
  }

  private def comment(i: Int): Int = getCharSafe(i) match {
    case '/' =>
      getCharSafe(i + 1) match {
        case '/' =>
          var curr = i + 2
          while (!atEof(curr) && getCharSafe(curr) != '\n') {
            curr += 1
          }
          curr
        case _ =>
          i
      }
    case _ =>
      i
  }

  // metaconfig specific end
}

object JsonConfParser extends Transformer[Input] {
  def transform[T](j: Input, f: Visitor[_, T]): T =
    new JsonConfParser(j).parse(f)
}
