package fix

import scalafix._
import scala.meta._

case class Metaconfig_v0_5(mirror: Mirror) extends SemanticRewrite(mirror) {
  def rewrite(ctx: RewriteCtx): Patch = {
    ctx.reporter.info(ctx.tree.syntax)
    ctx.reporter.info(ctx.tree.structure)
    Patch.empty
  }
}
