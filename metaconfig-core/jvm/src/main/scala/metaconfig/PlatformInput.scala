package metaconfig

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private[metaconfig] object PlatformInput {

  def readFile(path: String, charset: String): String =
    readFile(Paths.get(path), charset)

  def readFile(path: Path, charset: String): String =
    new String(Files.readAllBytes(path), charset)

}
