package metaconfig

import java.io.File
import metaconfig.generic.Surface

class DeriveSurfaceFileSuite extends munit.FunSuite {

  case class WithFile(file: File)
  test("toString") {
    val surface = generic.deriveSurface[WithFile]
    val obtained = surface.toString
    assertNoDiff(
      obtained,
      "Surface(List(List(Field(name=\"file\",tpe=\"File\",annotations=List(@TabCompleteAsPath()),underlying=List()))))"
    )
  }
}
