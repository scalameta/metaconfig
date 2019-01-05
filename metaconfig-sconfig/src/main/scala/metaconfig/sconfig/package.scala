package metaconfig

package object sconfig {
  implicit val sConfigMetaconfigParser = new MetaconfigParser {
    override def fromInput(input: Input): Configured[Conf] = input match {
      case Input.File(path, _, _) =>
        SConfig2Class.gimmeConfFromFile(path.toFile)
      case els =>
        SConfig2Class.gimmeConfFromString(new String(els.chars))
    }
  }
}
