package metaconfig

import org.ekrich.config.ConfigFactory

package object sconfig {
  implicit val sConfigMetaconfigParser: MetaconfigParser = (input: Input) => {
    // even if input is Input.File, let's not read it again and just parse as a string
    // also, for JS, ConfigFactory emulates java.io.File but all methods are unimplemented
    def config = ConfigFactory.parseString(input.text)
    SConfig2Class.gimmeSafeConf(config, Some(input))
  }
}
