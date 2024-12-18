package metaconfig

class ConfDynamicSuite extends munit.FunSuite {

  val conf: Conf.Obj = Conf.Obj(
    "x" -> Conf.Obj("c" -> Conf.Obj("d" -> Conf.Num(2))),
    "banana" -> Conf.Num(2),
    "kass" -> Conf.Str("boo"),
  )

  test("basic") {
    val obtained = conf.dynamic.x.c.d.asConf
    val expected = Configured.Ok(Conf.Num(2))
    assert(obtained == expected)
  }

  test("error") {
    assert(conf.dynamic.a.asConf.isNotOk)
    assert(conf.dynamic.x.c.d.e.asConf.isNotOk)
  }
  test("did you mean?") {
    conf.dynamic.banna.asConf match {
      case Configured.NotOk(err) =>
        assert(err.toString.contains("Did you mean 'banana'"))
      case Configured.Ok(value) =>
        fail("Expected \"Did you mean 'banana'\" error")
    }
  }
}
