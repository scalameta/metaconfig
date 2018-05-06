package metaconfig

import org.scalatest.FunSuite

class DeriveConfCodecSuite extends FunSuite {

  def check[T: ConfCodec](name: String, original: T): Unit = {
    test(name) {
      val conf = ConfEncoder[T].write(original)
      val obtained = ConfDecoder[T].read(conf).get
      assert(obtained == original)
    }
  }

  check[AllTheAnnotations](
    "basic",
    AllTheAnnotations()
  )

}
