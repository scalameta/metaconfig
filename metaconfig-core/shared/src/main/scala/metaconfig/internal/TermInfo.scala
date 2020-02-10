package metaconfig.internal

import scala.util.control.NonFatal

object TermInfo {
  def tputsColumns(fallback: Int = 80): Int = {
    import scala.sys.process._
    try List("tput", "cols").!!.toInt
    catch {
      case NonFatal(_) => fallback
    }
  }
}
