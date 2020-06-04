package mopt

import mopt.typesafeconfig.TypesafeConfig2Class

object Hocon extends MOptParser {
  override def fromInput(input: Input): Configured[Conf] = input match {
    case Input.File(path, _) =>
      TypesafeConfig2Class.gimmeConfFromFile(path.toFile)
    case Input.VirtualFile(path, text) =>
      TypesafeConfig2Class.gimmeConfFromStringFilename(path, text)
    case els =>
      TypesafeConfig2Class.gimmeConfFromString(new String(els.chars))
  }
}
