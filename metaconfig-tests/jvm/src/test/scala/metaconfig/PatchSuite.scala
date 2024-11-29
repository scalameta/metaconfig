package metaconfig

class PatchSuite extends munit.FunSuite {

  def check(original: Conf, revised: Conf, expected: String): Unit =
    test(original.toString()) {
      val obtained = Conf.printHocon(Conf.patch(original, revised))
      assertNoDiff(obtained, expected)
    }

  check(
    Conf.Obj(
      "a" -> Conf.Str("b"),
      "c" -> Conf.Str("d"), // ignored
    ),
    Conf.Obj("a" -> Conf.Str("c"), "e" -> Conf.Str("f")),
    """a = c
      |e = f
    """.stripMargin.trim,
  )

  check(
    Conf.Obj("a" -> Conf.Lst(Conf.Str("b"), Conf.Str("c"))),
    Conf.Obj("a" -> Conf.Lst(Conf.Str("c"), Conf.Str("d"))),
    """
      |a = [
      |  c
      |  d
      |]
    """.stripMargin.trim,
  )

  test("patch with key and empty val: delete") {
    val initial = Conf.Obj("a" -> Conf.Num(0), "b" -> Conf.Str("1"))
    val patched = Conf.applyPatch(initial, Conf.Obj("a" -> Conf.Obj.empty))
    assertEquals(patched, Conf.Obj("b" -> Conf.Str("1")))
  }

}
