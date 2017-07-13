package metaconfig

import org.scalameta.logger
import org.scalatest.FunSuite

class ConfDynamicTest extends FunSuite {

  val conf = Conf.Obj(
    "x" -> Conf.Obj("c" -> Conf.Obj("d" -> Conf.Num(2))),
    "banana" -> Conf.Num(2),
    "kass" -> Conf.Str("boo"))

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
    val Configured.NotOk(err) = conf.dynamic.banna.asConf
    assert(err.toString.contains("Did you mean 'banana'"))
  }
}
