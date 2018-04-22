package metaconfig

trait MetaconfigParser {
  def fromInput(input: Input): Configured[Conf]
}
