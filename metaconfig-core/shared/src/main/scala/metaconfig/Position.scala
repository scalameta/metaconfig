package metaconfig

sealed abstract class Position { pos =>
  def input: Input
  def start: Int
  def startLine: Int
  def startColumn: Int
  def end: Int
  def endLine: Int
  def endColumn: Int
  def text: String

  /** Returns true if this position encloses the other position */
  final def encloses(other: Position): Boolean =
    pos.start <= other.start &&
      pos.end > other.end

  /** Returns a formatted string of this position including filename/line/caret.
    */
  final def pretty(severity: String, message: String): String = {
    // Predef.augmentString = work around scala/bug#11125 on JDK 11
    val content = augmentString(lineContent).linesIterator
    val sb = new StringBuilder()
    sb.append(lineInput(severity, message))
      .append("\n")
      .append(content.next())
      .append("\n")
      .append(lineCaret)
      .append("\n")
    content.foreach { line =>
      sb.append(line)
        .append("\n")
    }
    sb.toString()
  }

  final def lineInput(severity: String, message: String): String = {
    val sev = if (severity.isEmpty) "" else s" $severity:"
    val msg = if (message.isEmpty) "" else s" $message"
    s"${pos.input.syntax}:${pos.startLine + 1}:${pos.startColumn}$sev$msg"
  }

  final def lineCaret: String = pos match {
    case Position.None => ""
    case _ =>
      val caret =
        if (pos.startLine == pos.endLine) "^" * (pos.end - pos.start + 1)
        else "^"
      " " * pos.startColumn + caret
  }

  final def lineContent: String = pos match {
    case Position.None => ""
    case range: Position.Range =>
      val start = range.start - range.startColumn
      val end = range.input.lineToOffset(range.endLine + 1) - 1
      Position.Range(range.input, start, end).text
  }
}

object Position {

  case object None extends Position {
    def input = Input.None
    def start = -1
    def startLine = -1
    def startColumn = -1
    def end = -1
    def endLine = -1
    def endColumn = -1
    def text = ""
    override def toString = "Position.None"
  }

  final case class Range(input: Input, start: Int, end: Int) extends Position {
    def startLine: Int = input.offsetToLine(start)
    def startColumn: Int = start - input.lineToOffset(startLine)
    def endLine: Int = input.offsetToLine(end)
    def endColumn: Int = end - input.lineToOffset(endLine)
    override def text = new String(input.chars, start, end - start)
    override def toString: String = s"[$start..$end) in $input"
  }

}
