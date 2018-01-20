package metaconfig

import cats.data.NonEmptyList
import cats.data.Validated
import io.circe.AccumulatingDecoder
import io.circe.Decoder
import io.circe.DecodingFailure
import metaconfig.internal.Settings

case class WithoutTypos[T](value: T)
object WithoutTypos {
  Decoder
  implicit def CirceToDeprecatedDecoder[A](
      implicit decoder: Decoder[A],
      settings: Settings[A]): AccumulatingDecoder[WithoutTypos[A]] =
    AccumulatingDecoder.instance[WithoutTypos[A]] { c =>
      decoder.accumulating.apply(c).andThen { ok =>
        val validNames = settings.allNames
        val typoFields = c.keys.getOrElse(Nil).toList.filter { name =>
          validNames.contains(name)
        }
        val failures = typoFields.map(typo =>
          DecodingFailure(invalidField(validNames, typo), c.history))
        failures match {
          case Nil => Validated.validNel(WithoutTypos(ok))
          case head :: tail =>
            Validated.Invalid(NonEmptyList(head, tail))
        }
      }
    }

  def invalidField(validFields: List[String], field: String): String = {
    val expected = s" Expected one of: ${validFields.mkString(", ")}"
    val hint =
      if (validFields.lengthCompare(3) <= 0) expected
      else {
        val closestField = validFields.sorted.minBy(levenshtein(field))
        s" Did you mean '$closestField' instead?$expected"
      }
    s"Unknown field '$field'." + hint
  }

  /** Levenshtein distance. Implementation based on Wikipedia's algorithm. */
  private def levenshtein(s1: String)(s2: String): Int = {
    val dist = Array.tabulate(s2.length + 1, s1.length + 1) { (j, i) =>
      if (j == 0) i else if (i == 0) j else 0
    }

    for (j <- 1 to s2.length; i <- 1 to s1.length)
      dist(j)(i) =
        if (s2(j - 1) == s1(i - 1))
          dist(j - 1)(i - 1)
        else
          dist(j - 1)(i)
            .min(dist(j)(i - 1))
            .min(dist(j - 1)(i - 1)) + 1

    dist(s2.length)(s1.length)
  }
}
