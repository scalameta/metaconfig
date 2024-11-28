package metaconfig

class DeriveConfDecoderExJVMSuite extends munit.FunSuite {

  def checkOkStr[T, A](confStr: String, out: A, in: T = null)(implicit
      loc: munit.Location,
      decoder: ConfDecoderExT[T, A],
  ): Unit = {
    val cfg = Input.String(confStr).parse(Hocon)
    cfg.andThen(decoder.read(Option(in), _)) match {
      case Configured.NotOk(err) => fail(err.toString)
      case Configured.Ok(obtained) => assertEquals[Any, Any](obtained, out)
    }
  }

  test("nested param 1") {
    checkOkStr(
      "b { param = 2 }",
      Nested(b = OneParam(2)),
      Nested(b = OneParam(42)), // ignored, reset from input
    )
  }

  test("nested param 2") {
    checkOkStr(
      """
        |a = 14
        |c {
        |  b { param = 4 }
        |  c {
        |    k3 { param = 3 }
        |  }
        |}
        |d {
        |  "+" = [{
        |    b { param = 40 }
        |  }]
        |}
        |""".stripMargin,
      Nested(
        a = 14,
        c = Nested2(a = "n2", b = OneParam(4), c = Map("k3" -> OneParam(3))),
        d = Seq(
          Nested2("n1", OneParam(2), Map("k1" -> OneParam(1))),
          Nested2(b = OneParam(40)),
        ),
      ),
      Nested(
        a = 32,
        c = Nested2(a = "n2", b = OneParam(80), c = Map("k2" -> OneParam(2))),
        d = Seq(Nested2("n1", OneParam(2), Map("k1" -> OneParam(1)))),
      ),
    )
  }

  test("nested param 3") {
    checkOkStr(
      """
        |e {
        |  a = "xxx"
        |  b {
        |    b { param = 3 }
        |    c {
        |      "+" = {
        |        k3 { param = 33 }
        |      }
        |    }
        |  }
        |}
        |""".stripMargin,
      Nested(e =
        Nested3(
          a = "xxx",
          b = Nested2(
            a = "zzz",
            b = OneParam(3),
            c = Map("k2" -> OneParam(2), "k3" -> OneParam(33)),
          ),
        ),
      ),
      Nested(e =
        Nested3(a = "yyy", b = Nested2(a = "zzz", c = Map("k2" -> OneParam(2)))),
      ),
    )
  }

}
