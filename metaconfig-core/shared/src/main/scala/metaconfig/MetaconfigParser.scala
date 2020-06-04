package metaconfig

import java.nio.file.Path

trait MetaconfigParser {
  def parseString[T: ConfDecoder](text: String): Configured[T] =
    parseInput(Input.String(text))
  def parseFilename[T: ConfDecoder](
      filename: String,
      text: String
  ): Configured[T] =
    parseInput(Input.VirtualFile(filename, text))
  def parseInput[T: ConfDecoder](input: Input): Configured[T] =
    fromInput(input).andThen(ConfDecoder[T].read)
  def fromInput(input: Input): Configured[Conf]
  final def fromString(string: String): Configured[Conf] =
    fromInput(Input.String(string))
  final def fromString(filename: String, string: String): Configured[Conf] =
    fromInput(Input.VirtualFile(filename, string))
  final def fromFile(path: Path): Configured[Conf] =
    fromInput(Input.File(path))
}
