package metaconfig.internal

object Levenshtein {

  def closestCandidate(query: String, candidates: Seq[String]): Option[String] = {
    val candidatesWithRatio = candidates.flatMap { candidate =>
      val levDist = distance(candidate)(query).toDouble
      val ratio = levDist / math.max(query.length(), candidate.length())
      if (ratio < 0.4) Some((candidate, ratio)) else None // Don't return candidate when difference is large.
    }
    val result = candidatesWithRatio.sortBy(_._2).headOption.map(_._1)
    result.orElse(prefixCandidate(query, candidates))
  }

  private def prefixCandidate(
      query: String,
      candidates: Seq[String],
  ): Option[String] = {
    val prefixCandidates = candidates.flatMap { candidate =>
      if (candidate.startsWith(query)) Some(candidate) else None
    }
    Option(prefixCandidates).filter(_.length == 1).map(_.head)
  }

  /** Levenshtein distance. Implementation based on Wikipedia's algorithm. */
  def distance(s1: String)(s2: String): Int = {
    val dist = Array.tabulate(s2.length + 1, s1.length + 1) { (j, i) =>
      if (j == 0) i else if (i == 0) j else 0
    }

    for (j <- 1 to s2.length; i <- 1 to s1.length) dist(j)(i) =
      if (s2(j - 1) == s1(i - 1)) dist(j - 1)(i - 1)
      else dist(j - 1)(i).min(dist(j)(i - 1)).min(dist(j - 1)(i - 1)) + 1

    dist(s2.length)(s1.length)
  }

}
