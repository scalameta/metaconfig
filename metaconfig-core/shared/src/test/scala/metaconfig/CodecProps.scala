package metaconfig

import metaconfig.generic.Surface
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalacheck.ScalacheckShapeless._

case class Inner(a: String = "a", b: Boolean = true)
object Inner {
  implicit val surface: Surface[Inner] = generic.deriveSurface[Inner]
  implicit val codec: ConfCodec[Inner] = generic.deriveCodec(Inner())
}

case class Outer(inner: Inner = Inner(), c: Int = 0)
object Outer {
  implicit val surface: Surface[Outer] = generic.deriveSurface
  implicit val codec: ConfCodec[Outer] = generic.deriveCodec(Outer())
}

class CodecProps extends Properties("Codec") {

  def checkRoundtrip[T: ConfCodec](a: T): Boolean = {
    val conf = ConfEncoder[T].write(a)
    val b = ConfDecoder[T].read(conf).get
    a == b
  }

  property("roundtrip AllTheAnnotations") = forAll { a: AllTheAnnotations =>
    checkRoundtrip(a)
  }

  property("roundtrip Outer") = forAll { a: Outer =>
    checkRoundtrip(a)
  }

}
