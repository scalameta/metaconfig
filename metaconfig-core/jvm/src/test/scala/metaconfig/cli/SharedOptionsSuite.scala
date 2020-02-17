package metaconfig.cli

import metaconfig.annotation.Inline

class SharedOptionsSuite extends BaseCliParserSuite {
  case class SharedOptions(
      common: String = ""
  )
  object SharedOptions {
    implicit val surface =
      metaconfig.generic.deriveSurface[SharedOptions]
    implicit val codec =
      metaconfig.generic.deriveCodec[SharedOptions](SharedOptions())
  }
  case class FirstOptions(
      a: Int = 41,
      @Inline shared: SharedOptions = SharedOptions()
  )
  object FirstOptions {
    implicit val surface =
      metaconfig.generic.deriveSurface[FirstOptions]
    implicit val codec =
      metaconfig.generic.deriveCodec[FirstOptions](FirstOptions())
  }
  case class SecondOptions(
      b: Int = 42,
      @Inline first: FirstOptions = FirstOptions(),
      @Inline shared: SharedOptions = SharedOptions()
  )
  object SecondOptions {
    implicit val surface =
      metaconfig.generic.deriveSurface[SecondOptions]
    implicit val codec =
      metaconfig.generic.deriveCodec[SecondOptions](SecondOptions())
  }

  val foo = SharedOptions("foo")
  check(
    "basic".ignore,
    List("--common", "foo"),
    SecondOptions(first = FirstOptions(shared = foo), shared = foo)
  )

}
