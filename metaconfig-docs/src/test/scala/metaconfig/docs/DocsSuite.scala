package metaconfig.docs

import metaconfig._
import metaconfig.annotation._

class DocsSuite extends munit.FunSuite {

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
    assert(clue(docs).contains("table"))
    assert(clue(docs).contains("th"))
    assert(clue(docs).contains("code"))
    assert(clue(docs).contains("country"))
    assert(clue(docs).contains("Iceland"))
    assert(clue(docs).contains("home.address"))
    assert(clue(docs).contains("home.country"))
    assert(clue(docs).contains("Default value"))
  }

}
