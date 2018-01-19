package metaconfig

import org.scalatest.FunSuite
import ConfError._
import metaconfig.Conf._
import org.langmeta.inputs.Input
import org.langmeta.inputs.Position

class ConfErrorSuite extends FunSuite {

  def check(name: String, error: => ConfError, expected: => String): Unit = {
    test(name) {
      val obtained = error.toString.trim
      assert(obtained == expected)
    }
  }

  check(
    "typeMismatch",
    typeMismatch("A", Str("B")),
    """Type mismatch;
      |  found    : String (value: "B")
      |  expected : A""".stripMargin
  )

  check(
    "typeMismatch2",
    typeMismatch("A", "B", "C"),
    """Type mismatch at 'C';
      |  found    : B
      |  expected : A""".stripMargin
  )

  check(
    "invalidFields",
    invalidFields(List("A"), List("B", "C")),
    "Invalid field: A. Expected one of B, C"
  )

  check(
    "missingField",
    missingField(Obj("foobar" -> Str("2"), "qux" -> Num(2)), "fuzbar"),
    """{"foobar": "2", "qux": 2} has no field 'fuzbar'. Did you mean 'foobar' instead?"""
  )

  check(
    "combine",
    msg("message 1").combine(msg("message 2")),
    """|2 errors
       |[E0] message 1
       |[E1] message 2""".stripMargin
  )

  check(
    "deprecated",
    deprecated("name", "Use field instead", "v1.0"),
    """Setting 'name' is deprecated since version v1.0. Use field instead""".stripMargin
  )

  check(
    "fileDoesNotExist",
    fileDoesNotExist("/path/to.txt"),
    """File /path/to.txt does not exist.""".stripMargin
  )

  check(
    "parseError", {
      val input = Input.VirtualFile(
        "foo.scala",
        """
          |object A {
          |  var x
          |}
        """.stripMargin
      )
      val i = input.value.indexOf('v')
      val pos = Position.Range(input, i, i + 2)
      parseError(pos, "No var")
    },
    """|foo.scala:3: error: No var
       |var x
       |  ^""".stripMargin
  )
}
