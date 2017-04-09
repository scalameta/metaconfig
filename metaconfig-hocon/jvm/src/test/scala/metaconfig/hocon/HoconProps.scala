package metaconfig
package hocon

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.scalacheck.Prop.forAll
import org.scalacheck._
import org.scalameta.logger

class HoconProps extends Properties("Compliance") {

  private def config2map(config: Config): Conf = {
    import scala.collection.JavaConverters._
    def loop(obj: Any): Conf = obj match {
      case map: java.util.Map[_, _] =>
        Conf.Obj(
          map.asScala.collect {
            case (key: String, value) => key -> loop(value)
          }.toList
        )
      case map: java.util.List[_] =>
        Conf.Lst(map.asScala.map(loop).toList)
      case e: String => Conf.Str(e)
      case e: Int => Conf.Num(e)
      case e: Boolean => Conf.Bool(e)
    }
    loop(config.resolve().root().unwrapped())
  }

  import Generators.argConfShow
  import ConfOps.sortKeys

  property("normalized") = forAll { conf: ConfShow =>
    val typesafeConf =
      sortKeys(config2map(ConfigFactory.parseString(conf.str)).normalize)
    val Configured.Ok(metaconfigConf) =
      Hocon2Class.gimmeConfig(conf.str).map(x => sortKeys(x.normalize))
    metaconfigConf == typesafeConf
  }
}
