package mopt

class EqualitySuite extends munit.FunSuite {
  def checkNotEqual(a: Conf, b: Conf): Unit =
    test("ne " + a.toString) {
      assertNotEquals(a, b)
    }

  def checkEqual(a: Conf, b: Conf): Unit =
    test("eq " + a.toString) {
      assertEquals(a, b)
    }

  checkNotEqual(
    Conf.Lst(Conf.Str("a"), Conf.Str("b")),
    Conf.Lst(Conf.Str("a"))
  )

  checkEqual(
    Conf.Str("a"),
    Conf.Str("a").withPos(Position.Range(Input.String("a"), 0, 1))
  )

  checkNotEqual(
    Conf.Str("b"),
    Conf.Str("c")
  )

  checkEqual(
    Conf.Obj("a" -> Conf.Str("b"), "b" -> Conf.Str("c")),
    Conf.Obj("b" -> Conf.Str("c"), "a" -> Conf.Str("b"))
  )

}
