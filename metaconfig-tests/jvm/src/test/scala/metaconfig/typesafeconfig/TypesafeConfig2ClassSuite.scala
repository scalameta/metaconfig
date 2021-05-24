package metaconfig.typesafeconfig

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import metaconfig.{Conf, Position}

class TypesafeConfig2ClassSuite extends munit.FunSuite {
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
    val expected: Conf = Conf.Obj(
      "a" -> Conf.Lst(
        Conf.Num(1),
        Conf.Str("2"),
        Conf.Bool(true)
      )
    )
    assertEquals(obtained, expected)
  }

  test("file not found") {
    val f = File.createTempFile("doesnotexist", "conf")
    f.delete()
    assert(TypesafeConfig2Class.gimmeConfFromFile(clue(f)).isNotOk)
  }

  test("null") {
    val obtained =
      TypesafeConfig2Class
        .gimmeConfFromString(
          """|keywords = [
             |  null
             |]""".stripMargin
        )
        .get
    val expected: Conf = Conf.Obj(
      "keywords" -> Conf.Lst(
        Conf.Null()
      )
    )
    assertEquals(obtained, expected)
  }

  test("include") {
    val dir = Files.createTempDirectory("include")

    val main = dir.resolve("main.conf")
    Files.write(
      main,
      """|a = [ 1 ]
         |include "included.conf"
         |c = bar
         |""".stripMargin.getBytes()
    )

    val included = dir.resolve("included.conf")
    Files.write(
      included,
      s"""|a = $${a} [
          |  "2",
          |  3,
          |  true
          |]
          |b = foo
          |""".stripMargin.getBytes()
    )

    val obtained = TypesafeConfig2Class.gimmeConfFromFile(main.toFile).get
    val expected: Conf = Conf.Obj(
      "a" -> Conf.Lst(
        Conf.Num(1),
        Conf.Str("2"),
        Conf.Num(3),
        Conf.Bool(true)
      ),
      "b" -> Conf.Str("foo"),
      "c" -> Conf.Str("bar")
    )
    assertEquals(obtained, expected)

    val obtainedObj = obtained.asInstanceOf[Conf.Obj]

    // declaration spread between main file and included file -> unknown position
    val aPos = obtainedObj.field("a").get.pos
    assertEquals(aPos, Position.None: Position)

    val bPos = obtainedObj.field("b").get.pos
    assertEquals(bPos.lineContent, "b = foo")

    val cPos = obtainedObj.field("c").get.pos
    assertEquals(cPos.lineContent, "c = bar")
  }
}
