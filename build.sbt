import com.typesafe.tools.mima.core._

import Extensions._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val smorg = "org.scalameta"
inThisBuild(List(
  // version is set dynamically by sbt-dynver, but let's adjust it
  version := {
    val curVersion = version.value
    def dynVer(out: sbtdynver.GitDescribeOutput): String = {
      def tagVersion = out.ref.dropPrefix
      if (out.isCleanAfterTag) tagVersion
      else if (System.getenv("CI") == null) s"$tagVersion-next-SNAPSHOT" // modified for local builds
      else if (out.commitSuffix.distance == 0) tagVersion
      else if (sys.props.contains("backport.release")) tagVersion
      else curVersion
    }
    dynverGitDescribeOutput.value.mkVersion(dynVer, curVersion)
  },
  useSuperShell := false,
  organization := smorg,
  licenses :=
    Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/scalameta/metaconfig")),
  autoAPIMappings := true,
  apiURL := Some(url("https://github.com/scalameta/metaconfig")),
  developers += Developer(
    "olafurpg",
    "Ólafur Páll Geirsson",
    "olafurpg@gmail.com",
    url("https://geirsson.com"),
  ),
  resolvers += Resolver.sonatypeCentralSnapshots,
  versionScheme := Some("early-semver"),
))

addCommandAlias(
  "scalafixAll",
  s"; ++$scala212 ; scalafixEnable ; all scalafix test:scalafix",
)
addCommandAlias(
  "scalafixCheckAll",
  s"; ++$scala212 ;  scalafixEnable ; scalafix --check ; test:scalafix --check",
)

addCommandAlias(
  "native-image",
  "; tests/graalvm-native-image:packageBin ; taskready",
)

commands += Command.command("taskready") { s =>
  import scala.sys.process._
  "afplay /System/Library/Sounds/Hero.aiff".!
  s
}

val languageAgnosticCompatibilityPolicy: ProblemFilter = (problem: Problem) => {
  val public = problem match {
    case problem: TemplateProblem => problem.ref.isPublic
    case problem: MemberProblem => problem.ref.isPublic
  }
  val fullName = problem.matchName.getOrElse("")
  val include = fullName.startsWith("metaconfig.")
  val exclude = fullName.contains(".internal.") ||
    fullName.startsWith("metaconfig.cli")
  public && include && !exclude
}

lazy val sharedSettings = Def.settings(
  scalacOptions ++= { if (isScala3.value) Nil else Seq("-Yrangepos") },
  scalacOptions += {
    if (isScala213.value || isScala3.value) "-Wunused:imports"
    else "-Ywarn-unused-import"
  },
  scalacOptions += "-deprecation",
  scalacOptions += "-Xfatal-warnings",
  scalacOptions ++= {
    if (isScala213.value) "-Wconf:cat=deprecation:is" :: Nil
    else if (isScala3.value) "-Wconf:cat=deprecation:silent" :: Nil
    else Nil
  },
  scalacOptions ++=
    { if (isScala3.value) Nil else "-Wconf:cat=feature:is" :: Nil },
  mimaBinaryIssueFilters += languageAgnosticCompatibilityPolicy,
  crossScalaVersions := ScalaVersions,
  scalaVersion := scala213,
)

lazy val mimaSettings = Def.settings(
  // the last tag, so the baseline cannot go stale; CI has to fetch tags for it
  mimaPreviousArtifacts :=
    previousStableVersion.value.map(smorg %% moduleName.value % _).toSet,
)

// sbt 2.x requires JDK 17+ to run, but our published artifacts must keep
// running on JDK 8. -release pins both the emitted bytecode version and the
// visible JDK API, so building on a newer JDK cannot leak newer APIs in.
// JVM-only: -release is meaningless for the Scala.js/Native back ends.
lazy val jvmReleaseSettings = Def.settings(
  scalacOptions ++= Seq("-release", "8"),
  javacOptions ++= Seq("--release", "8"),
)

lazy val sharedJSSettings = Def.settings(
  crossScalaVersions := ScalaVersions,
  // to support Node.JS functionality
  scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
)

LocalRootProject / publish / skip := true
disablePlugins(MimaPlugin)

lazy val depPaiges = libraryDependencies +=
  "org.typelevel" %%% "paiges-core" % "0.4.4"
def depScalacheck = libraryDependencies ++= List(
  "org.scalacheck" %%% "scalacheck" % "1.19.0",
  smorg %%% "munit-scalacheck" % "1.3.0" % Test,
)

def pprintSettings = Def.settings(
  sharedSettings,
  mimaSettings,
  moduleName := "metaconfig-pprint",
  libraryDependencies += "com.lihaoyi" %%% "fansi" % "0.5.1",
  libraryDependencies ++= {
    if (scalaVersion.value.startsWith("2.")) List(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    )
    else Nil
  },
)

lazy val pprint = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform).in(file("metaconfig-pprint"))
  .settings(pprintSettings)
  .jvmSettings(jvmReleaseSettings, jvmSources("metaconfig-pprint"))
  .jsSettings(jsSources("metaconfig-pprint"))
  .nativeSettings(nativeSources("metaconfig-pprint"))

def coreSettings = Def.settings(
  sharedSettings,
  mimaSettings,
  moduleName := "metaconfig-core",
  depPaiges,
  libraryDependencies +=
    "org.scala-lang.modules" %%% "scala-collection-compat" % "2.14.0",
  libraryDependencies += {
    val reflectVersion = if (isScala3.value) scala213 else scalaVersion.value
    "org.scala-lang" % "scala-reflect" % reflectVersion
  },
)

def coreJsSettings = Def.settings(
  sharedJSSettings,
  jsSources("metaconfig-core"),
  libraryDependencies +=
    (smorg %%% "io" % "4.17.3").cross(CrossVersion.for3Use2_13),
)

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform).in(file("metaconfig-core"))
  .settings(coreSettings).dependsOn(pprint)
  .nativeSettings(nativeSources("metaconfig-core"))
  .jvmSettings(jvmReleaseSettings, jvmSources("metaconfig-core"))
  .jsSettings(coreJsSettings)

def cliSettings = Def.settings(
  sharedSettings,
  mimaSettings,
  moduleName := "metaconfig-cli",
  depPaiges,
)

lazy val cli = crossProject(JVMPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform).in(file("metaconfig-cli")).settings(cliSettings)
  .jvmSettings(jvmReleaseSettings, jvmSources("metaconfig-cli"))
  .nativeSettings(nativeSources("metaconfig-cli")).dependsOn(core)

def typesafeSettings = Def.settings(
  sharedSettings,
  mimaSettings,
  jvmReleaseSettings,
  moduleName := "metaconfig-typesafe-config",
  description := "Integration for HOCON using typesafehub/config.",
  libraryDependencies += "com.typesafe" % "config" % "1.4.9",
)

lazy val typesafe = project.in(file("metaconfig-typesafe-config"))
  .settings(typesafeSettings).dependsOn(core.jvm)

def sconfigSettings = Def.settings(
  sharedSettings,
  mimaSettings,
  moduleName := "metaconfig-sconfig",
  description := "Integration for HOCON using ekrich/sconfig.",
  libraryDependencies += ("org.ekrich" %%% "sconfig" % "2.0.0").excludeAll(
    "org.scala-lang.modules" %
      s"scala-collection-compat_${scalaBinaryVersion.value}",
  ),
)

def sjavatime = Def
  .settings(libraryDependencies += "org.ekrich" %%% "sjavatime" % "1.5.0")

lazy val sconfig = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform).in(file("metaconfig-sconfig"))
  .settings(sconfigSettings)
  .jvmSettings(jvmReleaseSettings, jvmSources("metaconfig-sconfig"))
  .jsSettings(sharedJSSettings, jsSources("metaconfig-sconfig"), sjavatime)
  .nativeSettings(nativeSources("metaconfig-sconfig"), sjavatime).dependsOn(core)

def testsSettings = Def.settings(
  sharedSettings,
  publish / skip := true,
  Compile / packageDoc / publishArtifact := false,
  testFrameworks := List(new TestFramework("munit.Framework")),
  depScalacheck,
)

def testsJvmSettings = Def.settings(
  jvmSources("metaconfig-tests"),
  GraalVMNativeImage / mainClass := Some("metaconfig.tests.ExampleMain"),
  Compile / doc / sources := Seq.empty,
  libraryDependencies += {
    if (isScala3.value) "org.typelevel" %% "shapeless3-deriving" % "3.6.0"
    else "com.github.alexarchambault" %%% "scalacheck-shapeless_1.15" % "1.3.0"
  },
  graalVMNativeImageOptions ++= {
    val reflectionFile = (Compile / Keys.sourceDirectory).value / "graal" /
      "reflection.json"
    assert(reflectionFile.exists, "no such file: " + reflectionFile)
    List(
      "-H:+ReportUnsupportedElementsAtRuntime",
      "--initialize-at-build-time",
      "--initialize-at-run-time=metaconfig",
      "--no-server",
      "--enable-http",
      "--enable-https",
      "-H:EnableURLProtocols=http,https",
      "--enable-all-security-services",
      "--no-fallback",
      s"-H:ReflectionConfigurationFiles=$reflectionFile",
      "--allow-incomplete-classpath",
      "-H:+ReportExceptionStackTraces",
    )
  },
)

lazy val tests = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform).in(file("metaconfig-tests"))
  .disablePlugins(MimaPlugin).settings(testsSettings)
  .jsSettings(sharedJSSettings, jsSources("metaconfig-tests"))
  .nativeSettings(nativeSources("metaconfig-tests"))
  .jvmSettings(testsJvmSettings).jvmEnablePlugins(GraalVMNativeImagePlugin)
  .jvmConfigure(_.dependsOn(typesafe, cli.jvm)).dependsOn(core, sconfig)

def docsSettings = Def.settings(
  sharedSettings,
  depScalacheck,
  libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.13.1",
  publish / skip := true,
  dependencyOverrides +=
    smorg %% "metaconfig-typesafe-config" % (ThisBuild / version).value,
  moduleName := "metaconfig-docs",
  mdocVariables := Map(
    "VERSION" -> version.value.replaceFirst("\\+.*", ""),
    "SCALA_VERSION" -> scalaVersion.value,
  ),
  mdocOut := (ThisBuild / baseDirectory).value / "website" / "target" / "docs",
  mdocExtraArguments := List("--no-link-hygiene"),
  // mdoc's metaconfig might (and will eventually) lag behind the current version, causing eviction errors
  evictionErrorLevel := Level.Warn,
)

lazy val docs = project.in(file("metaconfig-docs")).settings(docsSettings)
  .dependsOn(core.jvm, typesafe, sconfig.jvm).enablePlugins(DocusaurusPlugin)
  .disablePlugins(MimaPlugin)
