package metaconfig

import munit.FunSuite

class HoconPrinterSuite extends FunSuite {

  def check(original: Conf, expected: String)(implicit
      loc: munit.Location
  ): Unit = {
    test(original.toString()) {
      val obtained = Conf.printHocon(original)
      assertNoDiff(obtained, expected)
    }
  }

  check(
    Conf.Obj(
      "a" -> Conf.Bool(true),
      "b" -> Conf.Null(),
      "c" -> Conf.Num(1),
      "d" -> Conf.Lst(Conf.Str("2"), Conf.Str("")),
      "e" -> Conf.Obj("f" -> Conf.Num(3)),
      "f.g" -> Conf.Num(2)
    ),
    """|a = true
      |b = null
      |c = 1
      |d = [
      |  "2"
      |  ""
      |]
      |e.f = 3
      |"f.g" = 2
      |""".stripMargin.trim
  )

  check(
    Conf.Obj(
      "a" -> Conf.Lst(Conf.Str("b.c"))
    ),
    """
      |a = [
      |  "b.c"
      |]
    """.stripMargin.trim
  )

  check(
    Conf.Obj(
      "a" -> Conf.Obj("b.c" -> Conf.Str("d"))
    ),
    """
      |a."b.c" = d
    """.stripMargin.trim
  )

}
