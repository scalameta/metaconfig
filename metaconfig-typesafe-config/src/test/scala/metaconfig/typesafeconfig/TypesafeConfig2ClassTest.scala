package metaconfig.typesafeconfig

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

import metaconfig.Conf
import metaconfig.Configured
import org.scalameta.logger
import org.scalatest.FunSuite

class TypesafeConfig2ClassTest extends FunSuite {
  test("basic") {
    val file = File.createTempFile("prefix", ".conf")
    Files.write(
      Paths.get(file.toURI),
      """|a.b = 2
         |a = [
         |  1,
         |  "2"
         |]
         |a += true""".stripMargin.getBytes()
    )
    val obtained = TypesafeConfig2Class.gimmeConfFromFile(file).get
    val expected = Conf.Obj(
      "a" -> Conf.Lst(
        Conf.Num(1),
        Conf.Str("2"),
        Conf.Bool(true)
      )
    )
    assert(obtained == expected)
  }

  test("file not found") {
    val f = File.createTempFile("doesnotexist", "conf")
    f.delete()
    assert(TypesafeConfig2Class.gimmeConfFromFile(f).isNotOk)
  }
}
