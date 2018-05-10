package metaconfig

import org.scalatest.FunSuite

class DeriveConfEncoderSuite extends FunSuite {

  def check[T: ConfEncoder](name: String, original: T, expected: Conf): Unit = {
    test(name) {
      val obtained = ConfEncoder[T].write(original)
      assert(obtained == expected)
    }
  }

  check(
    "noparam",
    NoParam(),
    Conf.Obj()
  )

  check(
    "annotations",
    AllTheAnnotations(
      number = 3,
      string = "foo",
      lst = List("lst")
    ),
    Conf.Obj(
      "number" -> Conf.Num(3),
      "string" -> Conf.Str("foo"),
      "lst" -> Conf.Lst(Conf.Str("lst"))
    )
  )

}
