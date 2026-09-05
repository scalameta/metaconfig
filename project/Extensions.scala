import sbt.Keys._
import sbt._

object Extensions {

  val scala212 = "2.12.21"

  val scala213 = "3.9.0"

  val scala3 = "3.3.8"

  val ScalaVersions = List(scala213, scala212, scala3)

  def isScala213 = Def.setting(scalaBinaryVersion.value == "2.13")
  def isScala3 = Def.setting(scalaVersion.value.startsWith("3."))

  // sbt gives every matrix cell a generated baseDirectory, so a source tree
  // starts from the matrix base instead
  def srcDir(tree: String) = Def.setting(matrixBase.value / tree / "src")

  // `in` records the file it is given, so the base can be relative
  private def matrixBase = Def.setting(IO.resolve(
    (ThisBuild / baseDirectory).value,
    projectMatrixBaseDirectory.value,
  ))

  // crossProject's layout, wired by hand: a matrix has one base directory, so
  // each cell names the trees it shares. Absent directories are harmless.
  private def roots(cfg: String, trees: Seq[String]) = Def.setting {
    val variants =
      List("scala", "java", if (isScala3.value) "scala-3" else "scala-2")
    val base = matrixBase.value
    for (tree <- trees; src = base / tree / "src" / cfg; variant <- variants)
      yield src / variant
  }

  private def unmanagedSources(trees: String*) = Def.settings(
    Compile / unmanagedSourceDirectories ++= roots("main", trees).value,
    Test / unmanagedSourceDirectories ++= roots("test", trees).value,
  )

  private def jvmSources =
    unmanagedSources("shared", "jvm", "js-jvm", "jvm-native")
  private def jsSources = unmanagedSources("shared", "js", "js-jvm", "js-native")
  private def nativeSources =
    unmanagedSources("shared", "native", "jvm-native", "js-native")

  // a test binary commits a bytemap of a sixteenth of its maximum heap before
  // it runs, and Windows cannot overcommit; the maximum defaults to the memory
  // of the machine, so a few binaries at once fill the commit limit
  private def nativeHeap = Def
    .settings(Test / envVars += "GC_MAXIMUM_HEAP_SIZE" -> "512m")

  implicit class ProjectMatrixExtensions(private val self: ProjectMatrix)
      extends AnyVal {

    def crossJvm(ss: Def.SettingsDefinition*): ProjectMatrix = self
      .jvmPlatform(ScalaVersions, jvmSources ++ ss.flatMap(_.settings))

    def crossJs(ss: Def.SettingsDefinition*): ProjectMatrix = self
      .jsPlatform(ScalaVersions, jsSources ++ ss.flatMap(_.settings))

    def crossNative(ss: Def.SettingsDefinition*): ProjectMatrix = self
      .nativePlatform(
        ScalaVersions,
        nativeSources ++ nativeHeap ++ ss.flatMap(_.settings),
      )

    // one row per Scala version, for rows that name their own dependencies
    def crossJvmRows(configure: String => Project => Project): ProjectMatrix =
      ScalaVersions.foldLeft(self) { (matrix, version) =>
        val func = configure(version).andThen(_.settings(jvmSources))
        matrix.jvmPlatform(Seq(version), Nil, func)
      }

  }

}
