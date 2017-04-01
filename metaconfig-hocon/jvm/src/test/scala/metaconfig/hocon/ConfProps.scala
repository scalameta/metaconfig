package metaconfig.hocon

import metaconfig.ConfShow
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck._
import org.scalameta.logger

class ConfProps extends Properties("Conf") {
  import metaconfig.Generators.argConfShow
  property(".normalise is idempotent") = forAll { show: ConfShow =>
    val Right(conf) = Hocon2Class.gimmeConfig(show.str)
    val diff = conf.normalize.diff(conf.normalize.normalize)
    if (diff.nonEmpty)
      logger.elem(conf.normalize, conf.normalize.normalize, diff)
    diff.isEmpty
  }
}
