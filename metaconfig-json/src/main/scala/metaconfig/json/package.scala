package metaconfig

import metaconfig.internal.JsonConverter
import scala.util.control.NonFatal

package object json {

  implicit val parser: MetaconfigParser = (input: Input) => {
    try {
      val js = JsonConverter.fromInput(input)
      Configured.ok(JsonConverter.toConf(js))
    } catch {
      case NonFatal(e) =>
        Configured.exception(e)
    }
  }

}
