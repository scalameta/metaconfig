package metaconfig

import scala.meta.inputs.Input

import java.io.File

package object typesafeconfig {
  implicit val typesafeConfigMetaconfigParser = new MetaconfigParser {
    override def fromInput(input: Input): Configured[Conf] = input match {
      case Input.File(path, _) =>
        TypesafeConfig2Class.gimmeConfFromFile(new File(path.absolute))
      case els =>
        TypesafeConfig2Class.gimmeConfFromString(new String(els.chars))
    }
  }
}
