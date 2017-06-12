package fix

import scalafix._
import scala.meta._

case object Metaconfig_v0_5 extends Rewrite {
  def rewrite(ctx: RewriteCtx): Patch = {
    ctx.reporter.info(ctx.tree.syntax)
    ctx.reporter.info(ctx.tree.structure)
    Patch.empty
  }
}
