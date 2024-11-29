package metaconfig

class ConfGetSuite extends munit.FunSuite {
  import Conf._

  test("getNested good") {
    val confA = Str("a")
    assertEquals(confA.getNested[String](), Configured.Ok("a"))

    val confB = Obj("b" -> confA)
    assertEquals(confB.getNested[String]("b"), Configured.Ok("a"))

    val confC = Obj("c" -> confB)
    assertEquals(confC.getNested[String]("c", "b"), Configured.Ok("a"))

    val confD = Obj("d" -> confC)
    assertEquals(confD.getNested[String]("d", "c", "b"), Configured.Ok("a"))
  }

  test("getNested fail") {
    val confA = Str("a")
    assertEquals(
      confA.getNested[Boolean]().getError.msg,
      """|Type mismatch;
         |  found    : String (value: "a")
         |  expected : Bool""".stripMargin,
    )

    assertEquals(
      confA.getNested[Boolean]("c").getError.msg,
      """|Type mismatch;
         |  found    : String (value: "a")
         |  expected : Conf.Obj with key 'c'""".stripMargin,
    )

    val confB = Obj("b" -> confA)
    assertEquals(
      confB.getNested[String]("c").getError.msg,
      """{"b": "a"} has no field 'c'.""",
    )
  }

}
