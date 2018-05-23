package metaconfig

import metaconfig.annotation._
import metaconfig.generic.Settings
import org.scalatest.FunSuite
import scala.meta.testkit.DiffAssertions

class JsonSchemaSuite extends FunSuite with DiffAssertions {

  def check[T: ConfEncoder: Settings](
      name: String,
      default: T,
      expected: String
  ): Unit = {
    test(name) {
      val schema = JsonSchema.generate[T](
        "title",
        "description",
        Some("url"),
        default
      )
      val obtained = ujson.write(schema, indent = 2)
      assertNoDiff(obtained, expected)
    }
  }

  case class Simple(
      @Description("A simple description")
      value: String,
      @Description("A repeated field")
      repeated: Seq[Int]
  )
  object Simple {
    implicit val encoder = generic.deriveEncoder[Simple]
    implicit val surface = generic.deriveSurface[Simple]
  }

  check(
    "Simple non-nested configs",
    Simple("Default Value", Seq(2)),
    """
      |{
      |  "$id": "url",
      |  "title": "title",
      |  "description": "description",
      |  "type": "object",
      |  "properties": {
      |    "value": {
      |      "title": "value",
      |      "description": "A simple description",
      |      "default": "Default Value",
      |      "required": false,
      |      "type": "string"
      |    },
      |    "repeated": {
      |      "title": "repeated",
      |      "description": "A repeated field",
      |      "default": [
      |        2
      |      ],
      |      "required": false,
      |      "type": "array"
      |    }
      |  }
      |}
    """.stripMargin
  )

  case class B(
      @Description("My value field")
      value: String
  )
  object B {
    implicit val encoder = generic.deriveEncoder[B]
    implicit val surface = generic.deriveSurface[B]
  }
  case class A(
      value: Int,
      @Description("Nested field")
      b: B
  )
  object A {
    implicit val encoder = generic.deriveEncoder[A]
    implicit val surface = generic.deriveSurface[A]
  }

  check(
    "Complex nested configuration objects",
    A(42, B("Hest")),
    """
      |{
      |  "$id": "url",
      |  "title": "title",
      |  "description": "description",
      |  "type": "object",
      |  "properties": {
      |    "value": {
      |      "title": "value",
      |      "description": null,
      |      "default": 42,
      |      "required": false,
      |      "type": "number"
      |    },
      |    "b": {
      |      "title": "b",
      |      "description": "Nested field",
      |      "default": {
      |        "value": "Hest"
      |      },
      |      "required": false,
      |      "type": "object",
      |      "properties": {
      |        "value": {
      |          "title": "value",
      |          "description": "My value field",
      |          "default": "Hest",
      |          "required": false,
      |          "type": "string"
      |        }
      |      }
      |    }
      |  }
      |}
    """.stripMargin
  )

}
