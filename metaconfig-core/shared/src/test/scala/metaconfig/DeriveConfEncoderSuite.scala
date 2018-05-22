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

  check(
    "iterable",
    IsIterable(Set(42)),
    Conf.Obj(
      "a" -> Conf.Lst(Conf.Num(42)),
      "b" -> Conf.Lst()
    )
  )

  check(
    "option",
    Option(32),
    Conf.Num(32)
  )

  check(
    "option2",
    HasOption(None),
    Conf.Obj(
      "b" -> Conf.Null()
    )
  )

}
