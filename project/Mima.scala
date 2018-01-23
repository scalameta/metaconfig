import com.typesafe.tools.mima.core._

// To learn more about mima, see https://github.com/typesafehub/migration-manager/wiki/sbt-plugin#basic-usage
object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    Seq(
//      ProblemFilters.exclude[DirectMissingMethodProblem](
//        "metaconfig.Configured.flatMap")
    )
  }
}
