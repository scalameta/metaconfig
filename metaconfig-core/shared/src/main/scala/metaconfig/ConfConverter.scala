package metaconfig

trait ConfConverter { self =>

  def convert(conf: Conf): Conf

  final def convert(conf: Configured[Conf]): Configured[Conf] = conf
    .map(self.convert)

}
