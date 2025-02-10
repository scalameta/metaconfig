package metaconfig

import java.nio.file.Path

import scala.meta.internal.io._

private[metaconfig] object PlatformInput {

  def readFile(path: String, charset: String): String = JSFs
    .readFileSync(path, charset)

  def readFile(path: Path, charset: String): String =
    readFile(path.toString, charset)

}
