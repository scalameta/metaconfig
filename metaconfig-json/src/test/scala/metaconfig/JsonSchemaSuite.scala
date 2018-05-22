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
      println(obtained)
      assertNoDiff(obtained, expected)
    }
  }

  case class Simple(
      @Description("A simple description")
      value: String
  )
  object Simple {
    implicit val encoder = generic.deriveEncoder[Simple]
    implicit val surface = generic.deriveSurface[Simple]
  }

  check(
    "Simple non-nested configs",
    Simple("Default Value"),
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
      |      "type": "string",
      |      "properties": {
      |
      |      }
      |    }
      |  }
      |}
    """.stripMargin
  )

  case class B(value: String)
  object B {
    implicit val encoder = generic.deriveEncoder[B]
    implicit val surface = generic.deriveSurface[B]
  }
  case class A(value: Int, b: B)
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
      |      "type": "number",
      |      "properties": {
      |
      |      }
      |    },
      |    "b": {
      |      "title": "b",
      |      "description": null,
      |      "default": {
      |        "value": "Hest"
      |      },
      |      "required": false,
      |      "type": "object",
      |      "properties": {
      |        "value": {
      |          "title": "value",
      |          "description": null,
      |          "default": "Hest",
      |          "required": false,
      |          "type": "string",
      |          "properties": {
      |
      |          }
      |        }
      |      }
      |    }
      |  }
      |}
    """.stripMargin
  )

}
