import scala.meta.inputs.Input

package object metaconfig {
  @deprecated("Use Conf.fromInput instead.", "0.6.0")
  implicit class XtensionInputToConf(input: Input)(
      implicit parser: MetaconfigParser) {
    def toConf: Configured[Conf] = parser.fromInput(input)
  }
}
