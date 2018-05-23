package metaconfig.internal

import java.nio.file.Paths
import scala.meta.testkit.DiffAssertions
import org.scalatest.FunSuite
import metaconfig.annotation._
import metaconfig._
import metaconfig.generic.Settings

case class Site(
    foo: String = "foo",
    custom: Map[String, String] = Map.empty
)
object Site {
  implicit val surface = generic.deriveSurface[Site]
  implicit val codec = generic.deriveCodec[Site](Site())
}

case class Options(
    @Description("The input directory to generate the fox site.")
    @ExtraName("i")
    in: String = Paths.get("docs").toString,
    @Description("The output directory to generate the fox site.")
    @ExtraName("o")
    out: String = Paths.get("target").resolve("fox").toString,
    cwd: String = Paths.get(".").toAbsolutePath.toString,
    repoName: String = "olafurpg/fox",
    repoUrl: String = "https://github.com/olafurpg/fox",
    title: String = "Fox",
    description: String = "My Description",
    googleAnalytics: List[String] = Nil,
    classpath: List[String] = Nil,
    cleanTarget: Boolean = false,
    baseUrl: String = "",
    encoding: String = "UTF-8",
    configPath: String = Paths.get("fox.conf").toString,
    remainingArgs: List[String] = Nil,
    site: Site = Site(),
    @Inline
    inlined: Site = Site()
)
object Options {
  implicit val surface = generic.deriveSurface[Options]
  implicit val codec: ConfCodec[Options] =
    generic.deriveCodec[Options](Options())
}

class BaseCliParserSuite extends FunSuite with DiffAssertions {
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
      expectedOptions: Options): Unit = {
    test(name) {
      val conf = Conf.parseCliArgs[Options](args).get
      val obtainedOptions = ConfDecoder[Options].read(conf).get
      val obtained = toString(obtainedOptions)
      val expected = toString(expectedOptions)
      assertNoDiff(obtained, expected)
    }
  }
}

class CliParserSuite extends BaseCliParserSuite {

  check(
    "empty",
    Nil,
    Options()
  )

  check(
    "boolean",
    "--clean-target" :: Nil,
    Options(cleanTarget = true)
  )

  check(
    "string",
    "--in" :: "in" :: Nil,
    Options(in = "in")
  )

  check(
    "string2",
    "--in" :: "in" :: "--out" :: "out" :: Nil,
    Options(in = "in", out = "out")
  )

  check(
    "kebab",
    "--base-url" :: "base-url" :: Nil,
    Options(baseUrl = "base-url")
  )

  check(
    "remainingArgs",
    "arg1" :: Nil,
    Options(remainingArgs = "arg1" :: Nil)
  )

  check(
    "flag+remainingArgs",
    "--clean-target" :: "arg1" :: Nil,
    Options(cleanTarget = true, remainingArgs = "arg1" :: Nil)
  )

  check(
    "arg+value+remainingArgs",
    "--in" :: "in" :: "arg1" :: Nil,
    Options(in = "in", remainingArgs = "arg1" :: Nil)
  )

  check(
    "extraName",
    "-i" :: "in" :: "arg1" :: Nil,
    Options(in = "in", remainingArgs = "arg1" :: Nil)
  )

  check(
    "nested",
    "--site.foo" :: "blah" :: Nil,
    Options(site = Site(foo = "blah"))
  )

  check(
    "map",
    "--site.custom.key" :: "value" :: Nil,
    Options(site = Site(custom = Map("key" -> "value")))
  )

  check(
    "map2",
    "--site.custom.key1" :: "value1" ::
      "--site.custom.key2" :: "value2" ::
      Nil,
    Options(
      site = Site(
        custom = Map(
          "key1" -> "value1",
          "key2" -> "value2"
        ))
    )
  )

  check(
    "inline",
    "--foo" :: "blah" ::
      "--inlined.custom.explicit" :: "boom" ::
      "--custom.bar" :: "buz" :: Nil,
    Options(
      inlined = Site(
        foo = "blah",
        custom = Map(
          "bar" -> "buz",
          "explicit" -> "boom"
        ))
    )
  )
}
