package metaconfig

import java.nio.file.{Files, Path, Paths}

private[metaconfig] object PlatformInput {

  def readFile(path: String, charset: String): String =
    readFile(Paths.get(path), charset)

  def readFile(path: Path, charset: String): String =
    new String(Files.readAllBytes(path), charset)

}
