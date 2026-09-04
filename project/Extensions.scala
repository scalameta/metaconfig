import sbt.Keys._
import sbt._

object Extensions {

  val scala212 = "2.12.21"

  val scala213 = "2.13.18"

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

  private def platformSources(
      platform: String,
      ss: Seq[Def.SettingsDefinition],
      platforms: String*,
  ) = unmanagedSources("shared" +: platform +: platforms *) ++
    ideSkip(platform) ++ ss.flatMap(_.settings)

  private def jvmSources(ss: Seq[Def.SettingsDefinition]) =
    platformSources("jvm", ss, "js-jvm", "jvm-native")
  private def jsSources(ss: Seq[Def.SettingsDefinition]) =
    platformSources("js", ss, "js-jvm", "js-native")
  private def nativeSources(ss: Seq[Def.SettingsDefinition]) =
    platformSources("native", ss, "jvm-native", "js-native")

  // a test binary commits a bytemap of a sixteenth of its maximum heap before
  // it runs, and Windows cannot overcommit; the maximum defaults to the memory
  // of the machine, so a few binaries at once fill the commit limit
  private def nativeHeap = Def
    .settings(Test / envVars += "GC_MAXIMUM_HEAP_SIZE" -> "512m")

  /* IntelliJ folds the source roots that matrix cells share into one module,
   * so the whole matrix compiles scala-2 and scala-3 together. Only IntelliJ's
   * importer reads `ide-skip-project`, so these properties change what the IDE
   * sees and never what sbt builds. */
  private val ideSkipProject = SettingKey[Boolean]("ide-skip-project")

  private val ideScala = {
    val prop = sys.props.getOrElse("ide.scala", "").trim
    if (prop.isEmpty) scala213 else prop
  }

  // the platforms to import besides the JVM, which the IDE always gets
  private val idePlatforms = {
    val prop = sys.props.getOrElse("ide.platform", "").trim
    if (prop.isEmpty) Set.empty else prop.split("\\s*,\\s*").toSet + "jvm"
  }

  private def ideSkip(platform: String) = Seq(ideSkipProject := {
    val versions = Set(scalaBinaryVersion.value, scalaVersion.value)
    !versions(ideScala) || idePlatforms.nonEmpty && !idePlatforms(platform)
  })

  // sbt runs a `;`-separated list; the leading separator is required
  def tasks(ts: Iterable[String]): String = ts.mkString("; ", "; ", "")

  private def idSuffix(v: String) = VirtualAxis.scalaABIVersion(v).idSuffix

  // `++<version>` selects no row, so every version gets its own alias. Cell ids
  // are generated, so the names are taken from them rather than spelled out.
  def testAliases(versions: Seq[String], matrices: ProjectMatrix*) = versions
    .flatMap { v =>
      def alias(name: String, f: ProjectMatrix => ProjectFinder) =
        addCommandAlias(
          s"test-$name-${idSuffix(v)}",
          tasks(matrices.map(m => s"${f(m)(v).id}/testFull")),
        )
      alias("jvm", _.jvm) ++ alias("js", _.js) ++ alias("native", _.native)
    }

  // every row of a version, whatever its platform: what a cross-build job runs
  def crossAliases(versions: Seq[String], matrices: ProjectMatrix*) = versions
    .flatMap { v =>
      val rows = matrices.flatMap(_.allProjects().collect {
        case (p, axes) if axes.contains(VirtualAxis.scalaABIVersion(v)) => p.id
      })
      def alias(name: String, task: String) = addCommandAlias(
        s"$name-${idSuffix(v)}",
        tasks(rows.map(id => s"$id/$task")),
      )
      alias("test", "testFull") ++ alias("compile", "Test/compile")
    }

  implicit class ProjectMatrixExtensions(private val self: ProjectMatrix)
      extends AnyVal {

    def crossJvm(ss: Def.SettingsDefinition*): ProjectMatrix = self
      .jvmPlatform(ScalaVersions, jvmSources(ss))

    def crossJs(ss: Def.SettingsDefinition*): ProjectMatrix = self
      .jsPlatform(ScalaVersions, jsSources(ss))

    def crossNative(ss: Def.SettingsDefinition*): ProjectMatrix = self
      .nativePlatform(ScalaVersions, nativeSources(ss) ++ nativeHeap)

    // a JVM row for a project laid out the standard way, not as a crossProject
    def crossJvmPlain: ProjectMatrix = self
      .jvmPlatform(ScalaVersions, ideSkip("jvm"))

    // one row per Scala version, for rows that name their own dependencies
    def crossJvmRows(configure: String => Project => Project): ProjectMatrix =
      ScalaVersions.foldLeft(self) { (matrix, version) =>
        val func = configure(version).andThen(_.settings(jvmSources(Nil)))
        matrix.jvmPlatform(Seq(version), Nil, func)
      }

  }

}
