package metaconfig.internal

import scala.util.control.NonFatal

object TermInfo {

  def screenWidth(): Int = {
    math.min(120, math.max(40, tputsColumns() - 20))
  }

  def tputsColumns(fallback: Int = 80): Int = {
    import scala.sys.process._
    val pathedTput =
      if (new java.io.File("/usr/bin/tput").exists()) "/usr/bin/tput"
      else "tput"
    try {
      val columns =
        Seq("sh", "-c", s"$pathedTput cols 2> /dev/tty").!!.trim.toInt
      columns.toInt
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        fallback
    }
  }
}
