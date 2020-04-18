package metaconfig.cli

import metaconfig.Hocon

class EchoOptionsSuite extends munit.FunSuite {

  def checkError(
      name: String,
      input: String,
      expectedError: String
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      val parsed = Hocon.parseFilename("hello.conf", input)(EchoOptions.decoder)
      assert(clue(parsed).isNotOk)(munit.Location.generate)
      assertNoDiff(
        parsed.toEither.left.get.toString,
        expectedError
      )

    }
  }

  checkError(
    "basic",
    "foo = bar",
    """|hello.conf:1:0 error: found option 'foo' which wasn't expected, or isn't valid in this context.
       |foo = ba
       |^
       |""".stripMargin
  )

  checkError(
    "aggregate",
    """|foo = bar
       |qux = bar
       |""".stripMargin,
    """|2 errors
       |[E0] hello.conf:1:0 error: found option 'foo' which wasn't expected, or isn't valid in this context.
       |foo = bar
       |^
       |
       |[E1] hello.conf:2:0 error: found option 'qux' which wasn't expected, or isn't valid in this context.
       |qux = bar
       |^
       |""".stripMargin
  )

}
