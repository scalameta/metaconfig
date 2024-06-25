package metaconfig

import metaconfig.Conf._

class DeriveConfDecoderExSuite extends munit.FunSuite {

  def checkError(
      name: String,
      obj: Conf,
      expected: String
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      ConfDecoderEx[AllTheAnnotations].read(None, obj) match {
        case Configured.NotOk(obtained) =>
          assertNoDiff(obtained.toString, expected)
        case Configured.Ok(obtained) =>
          fail(s"Expected error, obtained=$obtained")
      }
    }
  }

  def checkOk(name: String, obj: Conf, expected: AllTheAnnotations): Unit =
    test(name) {
      checkOk(obj, expected)
    }

  def checkOk[A](cfg: Conf, out: A, in: A = null)(implicit
      loc: munit.Location,
      decoder: ConfDecoderEx[A]
  ): Unit =
    decoder.read(Option(in), cfg) match {
      case Configured.NotOk(err) => fail(err.toString)
      case Configured.Ok(obtained) =>
        assertEquals[Any, Any](obtained, out)
    }

  private val number = "number" -> Num(42)
  private val string = "string" -> Str("42")

  checkError(
    "typo",
    Obj(number, "sttring" -> Str("42")),
    """|found option 'sttring' which wasn't expected, or isn't valid in this context.
      |	Did you mean 'string'?
      |""".stripMargin
  )

  checkError(
    "typo2",
    Obj(string, "nummbber" -> Str("42")),
    """|found option 'nummbber' which wasn't expected, or isn't valid in this context.
      |	Did you mean 'number'?
      |""".stripMargin
  )

  checkOk(
    "basic",
    Obj(
      "number" -> Num(42),
      "string" -> Str("42"),
      "lst" -> Lst(Str("43") :: Nil)
    ),
    AllTheAnnotations(42, "42", List("43"))
  )

  checkOk(
    "extraName",
    Obj("extraName" -> Num(33)),
    AllTheAnnotations(33, "string", List())
  )

  checkOk(
    "extraName2",
    Obj("extraName2" -> Num(33)),
    AllTheAnnotations(33, "string", List())
  )

  checkOk(
    "deprecatedName",
    Obj("deprecatedName" -> Num(33)),
    AllTheAnnotations(33, "string", List())
  )

  checkOk(
    "deprecatedName2",
    Obj("deprecatedName2" -> Num(33)),
    AllTheAnnotations(33, "string", List())
  )

  test("iterable") {
    val obtained =
      IsIterable.decoderEx
        .read(None, Obj("b" -> Conf.Lst(Conf.Str("33"))))
        .get
    assert(obtained == IsIterable(b = Iterable("33")))
  }

  test("one param") {
    val obtained =
      OneParam.decoderEx.read(Some(OneParam(42)), Obj("param" -> Num(2))).get
    val expected = OneParam(2)
    assert(obtained == expected)
  }

  test("no param") {
    val decoder = generic.deriveDecoderEx[NoParam](NoParam())
    val obtained = decoder.read(None, Obj("param" -> Num(2))).get
    val expected = NoParam()
    assert(obtained == expected)
  }

  test("either") {
    implicit val decoderOneParam: ConfDecoderEx[OneParam] =
      generic.deriveDecoderEx[OneParam](OneParam()).noTypos
    implicit val decoderHasOption: ConfDecoderEx[HasOption] =
      generic.deriveDecoderEx[HasOption](HasOption()).noTypos
    val either = implicitly[ConfDecoderEx[Either[OneParam, HasOption]]]
    assertEquals(
      either.read(None, Obj("param" -> Num(2))).get,
      Left(OneParam(2))
    )
    assertEquals(
      either.read(Some(Left(OneParam(1))), Obj("param" -> Num(2))).get,
      Left(OneParam(2))
    )
    assertEquals(
      either.read(Some(Right(HasOption())), Obj("param" -> Num(2))).get,
      Left(OneParam(2))
    )

    assertEquals(
      either.read(None, Obj("b" -> Num(2))).get,
      Right(HasOption(Some(2)))
    )
    assertEquals(
      either.read(Some(Left(OneParam(1))), Obj("b" -> Num(2))).get,
      Right(HasOption(Some(2)))
    )
    assertEquals(
      either.read(Some(Right(HasOption())), Obj("b" -> Num(2))).get,
      Right(HasOption(Some(2)))
    )

    def getMsg(c: Configured[_]) = c.toEither.left.get.msg

    assertEquals(
      getMsg(either.read(None, Obj("c" -> Num(3)))),
      "found option 'c' which wasn't expected, or isn't valid in this context."
    )
    assertEquals(
      getMsg(either.read(Some(Left(OneParam(1))), Obj("c" -> Num(3)))),
      "found option 'c' which wasn't expected, or isn't valid in this context."
    )
    assertEquals(
      getMsg(either.read(Some(Right(HasOption())), Obj("c" -> Num(3)))),
      "found option 'c' which wasn't expected, or isn't valid in this context."
    )
  }

  case class MissingSurface(b: Int)

  def checkOption(
      conf: Conf,
      expected: HasOption,
      state: HasOption = HasOption()
  )(implicit
      decoder: ConfDecoderEx[HasOption]
  ): Unit = {
    test("option-" + conf.toString) {
      val obtained = decoder.read(Some(state), conf).get
      assert(obtained == expected)
    }
  }
  checkOption(Obj("b" -> Num(2)), HasOption(Some(2)))
  checkOption(Obj("a" -> Num(2)), HasOption(None))
  checkOption(Obj("b" -> Null()), HasOption(None), HasOption(Some(2)))

}
