package metaconfig

import org.langmeta.inputs.Input

trait MetaconfigParser {
  def fromInput(input: Input): Configured[Conf]
}
