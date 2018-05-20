package metaconfig.internal

import metaconfig.Input
import org.scalatest.FunSuite
import ujson.Js
import ujson.ParsingFailedException
import ujson.Transformable

class JsonConfErrorSuite extends FunSuite {
  def checkError(path: String, original: String, expected: String): Unit = {
    test(path) {
      val e = intercept[ParsingFailedException] {
        val transformable =
          Transformable.fromTransformer[Input](
            Input.VirtualFile(path, original),
            JsonConfParser)
        transformable.transform(Js)
      }
      println(e.getMessage)
      assert(e.getMessage == expected)
    }
  }

  checkError(
    "eof",
    """{ "a" """,
    "exhausted input"
  )

  checkError(
    "colon",
    """{ "a" 1 """,
    """|colon:0:6 error: expected :
       |{ "a" 1
       |      ^
       |""".stripMargin
  )
}
