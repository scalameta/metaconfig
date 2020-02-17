package metaconfig.cli

import java.nio.file.Paths
import metaconfig.annotation._
import metaconfig._
import metaconfig.generic.Settings
import java.io.File
import munit.TestOptions

abstract class BaseCliParserSuite extends munit.FunSuite {

  def check[T: Settings: ConfDecoder: ConfEncoder](
      name: TestOptions,
      args: List[String],
      expectedOptions: T
  ): Unit = {
    test(name) {
      val conf = Conf.parseCliArgs[T](args).get
      val obtainedOptions = ConfDecoder[T].read(conf).get
      assertEquals(obtainedOptions, expectedOptions)
    }
  }

  def checkError(
      name: String,
      args: List[String],
      expected: String
  ): Unit = {
    test(name) {
      val options = Conf
        .parseCliArgs[Options](args)
        .andThen(_.as[Options])
      val obtained = options match {
        case Configured.Ok(value) => value.toString()
        case Configured.NotOk(error) => error.all.mkString("\n")
      }
      assertNoDiff(obtained, expected)
    }
  }
}
