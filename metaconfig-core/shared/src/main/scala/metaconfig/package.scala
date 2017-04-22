import scala.meta.inputs.Input

package object metaconfig {
  implicit class XtensionInputToConf(input: Input)(
      implicit parser: MetaconfigParser) {
    def toConf: Configured[Conf] = parser.fromInput(input)
  }
}
