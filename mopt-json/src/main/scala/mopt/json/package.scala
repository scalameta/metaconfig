package mopt

import mopt.internal.JsonConverter
import scala.util.control.NonFatal

package object json {

  implicit val parser: MOptParser = new MOptParser {
    override def fromInput(input: Input): Configured[Conf] = {
      try {
        val js = JsonConverter.fromInput(input)
        Configured.ok(JsonConverter.toConf(js))
      } catch {
        case NonFatal(e) =>
          Configured.exception(e)
      }
    }
  }

}
