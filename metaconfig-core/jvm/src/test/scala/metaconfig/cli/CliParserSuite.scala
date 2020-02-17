package metaconfig.cli

import java.nio.file.Paths
import metaconfig.annotation._
import metaconfig._
import metaconfig.generic.Settings
import java.io.File

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
    "repeated",
    List("--classpath", "a", "--classpath", "b"),
    Options().copy(classpath = List("a", "b"))
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
        )
      )
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
        )
      )
    )
  )

  check(
    "=",
    "--title=buzz" :: Nil,
    Options(title = "buzz")
  )

  check(
    "conf",
    "--conf.foo" :: "qux" :: Nil,
    Options(conf = Conf.Obj("foo" -> Conf.Str("qux")))
  )

  check(
    "conf2",
    "--conf.foo.bar" :: "qux" :: Nil,
    Options(conf = Conf.Obj("foo" -> Conf.Obj("bar" -> Conf.Str("qux"))))
  )

  check(
    "version",
    "--conf.VERSION" :: "1.0" :: Nil,
    Options(conf = Conf.Obj("VERSION" -> Conf.Str("1.0")))
  )

  check(
    "true",
    "--conf.x.y" :: "true" :: "--conf.z" :: "1.0" :: Nil,
    Options(
      conf = Conf.Obj(
        "x" -> Conf.Obj("y" -> Conf.Str("true")),
        "z" -> Conf.Str("1.0")
      )
    )
  )

  check(
    "positional-everywhere1",
    "positional1" :: "--clean-target" :: "positional2" :: Nil,
    Options(
      cleanTarget = true,
      remainingArgs = List("positional1", "positional2")
    )
  )

  check(
    "positional-everywhere2",
    "positional1" :: "--title" :: "buzz" :: "positional2" :: Nil,
    Options(
      title = "buzz",
      remainingArgs = List("positional1", "positional2")
    )
  )

  check(
    "positional-everywhere3",
    "positional1" :: "--title" :: "buzz" :: Nil,
    Options(
      title = "buzz",
      remainingArgs = List("positional1")
    )
  )

  checkError(
    "positional-everywhere4",
    "positional1" :: "--title" :: Nil,
    "the argument '--title' requires a value but none was supplied"
  )

  checkError(
    "positional-everywhere5",
    "positional1" :: "--titl" :: Nil,
    """|found argument '--titl' which wasn't expected, or isn't valid in this context.
       |	Did you mean '--title'?
       |""".stripMargin
  )

}
