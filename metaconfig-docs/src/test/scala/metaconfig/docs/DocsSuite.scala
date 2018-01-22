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
  }

  test("html") {
    val docs = Docs.html(User())
    pprint.log(docs)
  }

}
