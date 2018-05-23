package metaconfig

import java.nio.file.Path

trait MetaconfigParser {
  def fromInput(input: Input): Configured[Conf]
  final def fromString(string: String): Configured[Conf] =
    fromInput(Input.String(string))
  final def fromString(filename: String, string: String): Configured[Conf] =
    fromInput(Input.VirtualFile(filename, string))
  final def fromFile(path: Path): Configured[Conf] =
    fromInput(Input.File(path))
}
