package metaconfig

import metaconfig.Conf._

class DeriveConfDecoderSuite extends munit.FunSuite {

  def checkError(
      name: String,
      obj: Conf,
      expected: String
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      ConfDecoder[AllTheAnnotations].read(obj) match {
        case Configured.NotOk(obtained) =>
          assertNoDiff(obtained.toString, expected)
        case Configured.Ok(obtained) =>
          fail(s"Expected error, obtained=$obtained")
      }
    }
  }

  def checkOk(name: String, obj: Conf, expected: AllTheAnnotations): Unit = {
    test(name) {
      ConfDecoder[AllTheAnnotations].read(obj) match {
        case Configured.NotOk(err) =>
          fail(err.toString)
        case Configured.Ok(obtained) =>
          assert(obtained == expected)
      }
    }
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
      ConfDecoder[IsIterable].read(Obj("b" -> Conf.Lst(Conf.Str("33")))).get
    assert(obtained == IsIterable(b = Iterable("33")))
  }

  test("one param") {
    val decoder = generic.deriveDecoder[OneParam](OneParam(42))
    val obtained = decoder.read(Obj("param" -> Num(2))).get
    val expected = OneParam(2)
    assert(obtained == expected)
  }

  test("no param") {
    val decoder = generic.deriveDecoder[NoParam](NoParam())
    val obtained = decoder.read(Obj("param" -> Num(2))).get
    val expected = NoParam()
    assert(obtained == expected)
  }

  test("either") {
    implicit val decoderOneParam: ConfDecoder[OneParam] =
      generic.deriveDecoder[OneParam](OneParam()).noTypos
    implicit val decoderHasOption: ConfDecoder[HasOption] =
      generic.deriveDecoder[HasOption](HasOption()).noTypos
    val either = implicitly[ConfDecoder[Either[OneParam, HasOption]]]
    assertEquals(
      either.read(Obj("param" -> Num(2))).get,
      Left(OneParam(2))
    )
    assertEquals(
      either.read(Obj("b" -> Num(2))).get,
      Right(HasOption(Some(2)))
    )
    assertEquals(
      either.read(Obj("c" -> Num(3))).toEither.left.get.msg,
      "found option 'c' which wasn't expected, or isn't valid in this context."
    )
  }

  case class MissingSurface(b: Int)

  def checkOption(conf: Conf, expected: HasOption)(implicit
      decoder: ConfDecoder[HasOption]
  ): Unit = {
    test("option-" + conf.toString) {
      val obtained = decoder.read(conf).get
      assert(obtained == expected)
    }
  }
  checkOption(Obj("b" -> Num(2)), HasOption(Some(2)))
  checkOption(Obj("a" -> Num(2)), HasOption(None))
  checkOption(Obj("b" -> Null()), HasOption(None))(
    generic.deriveDecoder[HasOption](HasOption(Some(2)))
  )

}
