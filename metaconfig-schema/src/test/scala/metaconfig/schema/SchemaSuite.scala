package metaconfig.schema

import metaconfig._
import metaconfig.annotation._
import ujson._

class SchemaSuite extends org.scalatest.FunSuite {

  test("Simple non-nested configs") {

    case class Simple(
        @Description("A simple description")
        value: String
    )

    implicit val simpleSurface = generic.deriveSurface[Simple]

    val schema = Schema.schema(
      title = "Simple title",
      description = "Simple description",
      url = None,
      default = Simple("Default Value"))

    val expected = Js.Obj(
      "$id" -> Js.Null,
      "title" -> Js.Str("Simple title"),
      "description" -> Js.Str("Simple description"),
      "type" -> "object",
      "properties" -> Js.Obj(
        "value" -> Js.Obj(
          "title" -> Js.Str("value"),
          "description" -> "A simple description",
          "default" -> Js.Str("Default Value"),
          "required" -> Js.False,
          "type" -> "string",
          "properties" -> Js.Obj()
        )
      )
    )

    assert(schema == expected)
  }

  test("Complex nested configuration objects") {

    case class A(value: Int, b: B)
    case class B(value: String)

    implicit val bSurface = generic.deriveSurface[B]
    implicit val aSurface = generic.deriveSurface[A]

    val schema = Schema.schema(
      title = "Complex title",
      description = "Complex description",
      url = None,
      default = A(42, B("Hest")))

    val expected = Js.Obj(
      "$id" -> Js.Null,
      "title" -> Js.Str("Complex title"),
      "description" -> Js.Str("Complex description"),
      "type" -> "object",
      "properties" -> Js.Obj(
        "value" -> Js.Obj(
          "title" -> Js.Str("value"),
          "description" -> Js.Null,
          "default" -> Js.Num(42),
          "required" -> Js.False,
          "type" -> "int",
          "properties" -> Js.Obj()
        ),
        "b" -> Js.Obj(
          "title" -> Js.Str("b"),
          "description" -> Js.Null,
          "default" -> Js.Null,
          "required" -> Js.False,
          "type" -> "object",
          "properties" -> Js.Obj(
            "value" -> Js.Obj(
              "title" -> Js.Str("value"),
              "description" -> Js.Null,
              "default" -> Js.Str("Hest"),
              "required" -> Js.False,
              "type" -> "string",
              "properties" -> Js.Obj()
            )
          )
        )
      )
    )

    assert(schema == expected)
  }

}
