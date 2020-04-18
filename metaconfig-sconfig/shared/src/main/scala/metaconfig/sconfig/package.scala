package metaconfig

package object sconfig {
  implicit val sConfigMetaconfigParser: MetaconfigParser =
    new MetaconfigParser {
      override def fromInput(input: Input): Configured[Conf] = input match {
        case Input.File(path, _) =>
          SConfig2Class.gimmeConfFromFile(path.toFile)
        case els =>
          SConfig2Class.gimmeConfFromString(new String(els.chars))
      }
    }
}
