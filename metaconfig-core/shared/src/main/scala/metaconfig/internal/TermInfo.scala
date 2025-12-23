package metaconfig.internal

import java.nio.file.{Files, Paths}

import scala.util.Try

object TermInfo {

  def screenWidth(upperBound: Int = 100, lowerBound: Int = 40): Int = math
    .min(upperBound, math.max(lowerBound, tputsColumns() - 20))

  def tputsColumns(fallback: Int = 80): Int = {
    import scala.sys.process._
    val exists = Files.exists(Paths.get("/usr/bin/tput"))
    val pathedTput = if (exists) "/usr/bin/tput" else "tput"
    Try(Seq("sh", "-c", s"$pathedTput cols 2> /dev/tty").!!.trim.toInt)
      .getOrElse(fallback)
  }
}
