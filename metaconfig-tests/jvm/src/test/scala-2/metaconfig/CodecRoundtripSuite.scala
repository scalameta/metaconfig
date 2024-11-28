package metaconfig

import org.scalacheck.Prop.forAll
import org.scalacheck.ScalacheckShapeless._

class CodecRoundtripSuite extends munit.ScalaCheckSuite {

  def checkRoundtrip[T: ConfCodec](a: T): Boolean = {
    val conf = ConfEncoder[T].write(a)
    val b = ConfDecoder[T].read(conf).get
    a == b
  }

  property("roundtrip AllTheAnnotations") {
    forAll((a: AllTheAnnotations) => checkRoundtrip(a))
  }

  property("roundtrip Outer")(forAll((a: Outer) => checkRoundtrip(a)))

}
