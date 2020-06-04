package mopt.internal

import scala.util.control.NonFatal
import java.nio.file.Files
import java.nio.file.Paths

object TermInfo {

  def screenWidth(upperBound: Int = 100, lowerBound: Int = 40): Int = {
    math.min(upperBound, math.max(lowerBound, tputsColumns() - 20))
  }

  def tputsColumns(fallback: Int = 80): Int = {
    import scala.sys.process._
    val pathedTput =
      if (Files.exists(Paths.get("/usr/bin/tput"))) "/usr/bin/tput"
      else "tput"
    try {
      val columns =
        Seq("sh", "-c", s"$pathedTput cols 2> /dev/tty").!!.trim.toInt
      columns.toInt
    } catch {
      case NonFatal(_) =>
        fallback
    }
  }
}
