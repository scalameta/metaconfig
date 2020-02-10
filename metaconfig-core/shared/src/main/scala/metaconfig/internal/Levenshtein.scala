package metaconfig.internal

object Levenshtein {

  def closestCandidate(
      query: String,
      candidates: Seq[String]
  ): Option[String] = {
    if (candidates.isEmpty) {
      None
    } else {
      val candidate = candidates.sortBy(distance(query)).head
      val maxLength = query.length() + candidate.length()
      val minDifference = math.abs(query.length() - candidate.length())
      val difference = distance(candidate)(query).toDouble - minDifference
      val ratio = difference.toDouble /
        math.min(query.length(), candidate.length())
      if (ratio < 0.4) Some(candidate)
      else None // Don't return candidate when difference is large.
    }
  }

  /** Levenshtein distance. Implementation based on Wikipedia's algorithm. */
  def distance(s1: String)(s2: String): Int = {
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
