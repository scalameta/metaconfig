package metaconfig.cli

import metaconfig.generic.Surface
import metaconfig.ConfEncoder
import metaconfig.Conf
import org.typelevel.paiges.Doc

object VersionCommand extends Command[Unit]("version") {
  override def extraNames: List[String] = List("-v", "--version", "-version")
  override def description: Doc = Doc.paragraph("Show version information")
  def run(value: Unit, app: CliApp): Int = {
    app.out.println(app.version)
    0
  }
}
