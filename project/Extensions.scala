import sbt.Keys._
import sbt._

object Extensions {

  val scala212 = "2.12.21"

  val scala213 = "2.13.18"

  val scala3 = "3.3.8"

  val ScalaVersions = List(scala213, scala212, scala3)

  def isScala213 = Def.setting(scalaBinaryVersion.value == "2.13")
  def isScala3 = Def.setting(scalaVersion.value.startsWith("3."))

  def srcWithRoot(root: File, dir: String) = root / dir / "src"

  // crossProject's layout, wired by hand: a matrix has one base directory, so
  // each cell names the trees it shares. Absent directories are harmless.
  private def roots(name: String, cfg: String, dirs: String*) = Def.setting {
    val variants =
      List("scala", "java", if (isScala3.value) "scala-3" else "scala-2")
    val root = (ThisBuild / baseDirectory).value / name
    for (dir <- dirs; base = srcWithRoot(root, dir) / cfg; variant <- variants)
      yield base / variant
  }

  private def unmanagedSources(name: String, dirs: String*) = Def.settings(
    Compile / unmanagedSourceDirectories ++= roots(name, "main", dirs: _*).value,
    Test / unmanagedSourceDirectories ++= roots(name, "test", dirs: _*).value,
  )

  def jvmSources(name: String) =
    unmanagedSources(name, "shared", "jvm", "js-jvm", "jvm-native")
  def jsSources(name: String) =
    unmanagedSources(name, "shared", "js", "js-jvm", "js-native")
  def nativeSources(name: String) =
    unmanagedSources(name, "shared", "native", "jvm-native", "js-native")

}
