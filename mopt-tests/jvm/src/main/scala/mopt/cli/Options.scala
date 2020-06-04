package mopt.cli

import java.nio.file.Paths
import mopt.annotation._
import mopt._
import mopt.generic.Surface

case class Options(
    @Description("The input directory to generate the fox site.")
    @ExtraName("i")
    in: String = Paths.get("docs").toString,
    @Description("The output directory to generate the fox site.")
    @ExtraName("o")
    out: String = Paths.get("target").resolve("fox").toString,
    cwd: String = Paths.get(".").toAbsolutePath.toString,
    repoName: String = "olafurpg/fox",
    repoUrl: String = "https://github.com/olafurpg/fox",
    title: String = "Fox",
    description: String = "My Description",
    googleAnalytics: List[String] = Nil,
    classpath: List[String] = Nil,
    cleanTarget: Boolean = false,
    defaultTrue: Boolean = true,
    defaultFalse: Boolean = false,
    noFlipDefaultTrue: Boolean = true,
    noFlipDefaultFalse: Boolean = false,
    conflict: Boolean = false,
    noConflict: Boolean = false,
    @Description("")
    baseUrl: String = "",
    encoding: String = "UTF-8",
    @Section("Advanced")
    configPath: String = Paths.get("fox.conf").toString,
    remainingArgs: List[String] = Nil,
    conf: Conf = Conf.Obj(),
    site: Site = Site(),
    @Inline
    inlined: Site = Site(),
    @Hidden // should not appear in --help
    hidden: Int = 87
)
object Options {
  implicit val surface: Surface[Options] = generic.deriveSurface[Options]
  implicit val codec: ConfCodec[Options] =
    generic.deriveCodec[Options](Options())
}
