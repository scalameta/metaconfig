package metaconfig.cli

import java.nio.file.Paths
import metaconfig.annotation._
import metaconfig._
import metaconfig.generic.Settings
import java.io.File

class BaseCliParserSuite extends munit.FunSuite {
  val settings = Settings[Options]
  def toString(options: Options): String = {
    settings.settings
      .zip(options.productIterator.toList)
      .map {
        case (s, v) =>
          s"${s.name} = $v"
      }
      .mkString("\n")
  }
  def check(
      name: String,
      args: List[String],
      expectedOptions: Options
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      val conf = Conf.parseCliArgs[Options](args).get
      val obtainedOptions = ConfDecoder[Options].read(conf).get
      val obtained = toString(obtainedOptions)
      val expected = toString(expectedOptions)
      assertNoDiff(obtained, expected)
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
