package mopt

class DeriveConfCodecSuite extends munit.FunSuite {

  def check[T: ConfCodec](name: String, original: T): Unit = {
    test(name) {
      val conf = ConfEncoder[T].write(original)
      val obtained = ConfDecoder[T].read(conf).get
      assertEquals(obtained, original)
    }
  }

  check[AllTheAnnotations](
    "basic",
    AllTheAnnotations()
  )

}
