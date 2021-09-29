package metaconfig
import org.scalacheck._
import shapeless3.deriving.*

given [A](using inst: K0.ProductInstances[Arbitrary, A]): Arbitrary[A] =
  Arbitrary(inst.construct([t] => (ma: Arbitrary[t]) => ma.arbitrary.sample.get))
