package metaconfig.docs

import metaconfig._
import metaconfig.annotation._

class DocsSuite extends org.scalatest.FunSuite {

  case class Home(
      address: String = "blah",
      @Description("Original country")
      country: String = "Iceland"
  )
  object Home {
    implicit val surface = generic.deriveSurface[Home]
    implicit val codec = generic.deriveCodec[Home](Home())
  }

  case class User(
      @Description("Name description")
      name: String = "John",
      @Description("Age description")
      age: Int = 42,
      home: Home = Home()
  )
  object User {
    implicit val surface = generic.deriveSurface[User]
    implicit val codec = generic.deriveCodec[User](User())
  }

  test("html") {
    val docs = Docs.html(User())
    assert(docs.contains("table"))
    assert(docs.contains("th"))
    assert(docs.contains("code"))
    assert(docs.contains("country"))
    assert(docs.contains("Iceland"))
    assert(docs.contains("home.address"))
    assert(docs.contains("home.country"))
    assert(docs.contains("Default value"))
  }

}
