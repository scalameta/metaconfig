package metaconfig.sconfig

import metaconfig.Input

import java.net.URL
import java.nio.file.Paths

object PlatformFileOps {

  def fromURL(url: URL): Option[Input] = Some(Input.File(Paths.get(url.toURI)))

}
