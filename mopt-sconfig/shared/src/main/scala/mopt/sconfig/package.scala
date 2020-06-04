package mopt

package object sconfig {
  implicit val sConfigMOptParser: MOptParser =
    new MOptParser {
      override def fromInput(input: Input): Configured[Conf] = input match {
        case Input.File(path, _) =>
          SConfig2Class.gimmeConfFromFile(path.toFile)
        case els =>
          SConfig2Class.gimmeConfFromString(new String(els.chars))
      }
    }
}
