import com.typesafe.tools.mima.core._

import sbtcrossproject.CrossPlugin.autoImport.crossProject

lazy val V = new {
  def munit = "1.2.0"
  def scalacheck = "1.19.0"
}
val scala212 = "2.12.20"

val scala213 = "2.13.18"

val scala3 = "3.3.7"

def isScala213 = Def.setting(scalaBinaryVersion.value == "2.13")
def isScala3 = Def.setting(scalaVersion.value.startsWith("3."))

val smorg = "org.scalameta"
val Scala2Versions = List(scala213, scala212)
val ScalaVersions = scala3 :: Scala2Versions
inThisBuild(List(
  // version is set dynamically by sbt-dynver, but let's adjust it
  version := {
    val curVersion = version.value
    def dynVer(out: sbtdynver.GitDescribeOutput): String = {
      def tagVersion = out.ref.dropPrefix
      if (out.isCleanAfterTag) tagVersion
      else if (System.getProperty("CI") == null) s"$tagVersion-next-SNAPSHOT" // modified for local builds
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
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
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
  val (ref, fullName) = problem match {
    case problem: TemplateProblem => (problem.ref, problem.ref.fullName)
    case problem: MemberProblem => (problem.ref, problem.ref.fullName)
  }
  val public = ref.isPublic
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
  mimaPreviousArtifacts := Set("com.geirsson" %% moduleName.value % "0.9.10"),
)

lazy val sharedJSSettings = Def.settings(
  crossScalaVersions := Scala2Versions,
  // to support Node.JS functionality
  scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
)

publish / skip := true
disablePlugins(MimaPlugin)

lazy val depPaiges = libraryDependencies +=
  "org.typelevel" %%% "paiges-core" % "0.4.4"
def depScalacheck = libraryDependencies ++= List(
  "org.scalacheck" %%% "scalacheck" % V.scalacheck,
  smorg %%% "munit-scalacheck" % V.munit % Test,
)

lazy val pprint = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-pprint")).settings(
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

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-core")).settings(
    sharedSettings,
    mimaSettings,
    moduleName := "metaconfig-core",
    depPaiges,
    libraryDependencies +=
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.14.0",
  ).settings(
    libraryDependencies += {
      val reflectVersion = if (isScala3.value) scala213 else scalaVersion.value
      "org.scala-lang" % "scala-reflect" % reflectVersion
    },
    Compile / unmanagedSourceDirectories += {
      // TODO: why isn't sbt-crossproject adding epoch scala version
      // sources? it should
      val scalaMajor = if (isScala3.value) "scala-3" else "scala-2"
      baseDirectory.value / "shared" / "src" / "main" / scalaMajor
    },
  ).dependsOn(pprint).jsSettings(
    sharedJSSettings,
    libraryDependencies += smorg %%% "io" % "4.13.10" cross
      CrossVersion.for3Use2_13,
  )

lazy val cli = crossProject(JVMPlatform, NativePlatform)
  .in(file("metaconfig-cli")).settings(
    sharedSettings,
    mimaSettings,
    moduleName := "metaconfig-cli",
    depPaiges,
  ).dependsOn(core)

lazy val typesafe = project.in(file("metaconfig-typesafe-config")).settings(
  sharedSettings,
  mimaSettings,
  moduleName := "metaconfig-typesafe-config",
  description := "Integration for HOCON using typesafehub/config.",
  libraryDependencies += "com.typesafe" % "config" % "1.4.5",
).dependsOn(core.jvm)

lazy val sconfig = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-sconfig")).settings(
    sharedSettings,
    mimaSettings,
    moduleName := "metaconfig-sconfig",
    description := "Integration for HOCON using ekrich/sconfig.",
    libraryDependencies += ("org.ekrich" %%% "sconfig" % "1.12.0").excludeAll(
      "org.scala-lang.modules" %
        s"scala-collection-compat_${scalaBinaryVersion.value}",
    ),
  ).platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies += "org.ekrich" %%% "sjavatime" % "1.4.0",
  ).jsSettings(sharedJSSettings).dependsOn(core)

lazy val tests = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-tests")).disablePlugins(MimaPlugin).settings(
    sharedSettings,
    publish / skip := true,
    Compile / packageDoc / publishArtifact := false,
    testFrameworks := List(new TestFramework("munit.Framework")),
    depScalacheck,
  ).jsSettings(sharedJSSettings).jvmSettings(
    GraalVMNativeImage / mainClass := Some("metaconfig.tests.ExampleMain"),
    Compile / doc / sources := Seq.empty,
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2.")) Seq(
        "com.github.alexarchambault" %%% "scalacheck-shapeless_1.15" % "1.3.0",
      )
      else Seq("org.typelevel" %% "shapeless3-deriving" % "3.5.0")
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
  ).jvmEnablePlugins(GraalVMNativeImagePlugin)
  .jvmConfigure(_.dependsOn(typesafe, cli.jvm)).dependsOn(core, sconfig)

lazy val docs = project.in(file("metaconfig-docs")).settings(
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
).dependsOn(core.jvm, typesafe, sconfig.jvm).enablePlugins(DocusaurusPlugin)
  .disablePlugins(MimaPlugin)
