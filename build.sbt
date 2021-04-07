import java.util.Date
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import com.typesafe.tools.mima.core._

lazy val V = new {
  def munit = "0.7.23"
}
val scala212 = "2.12.13"
val scala213 = "2.13.5"
val ScalaVersions = List(scala212, scala213)
inThisBuild(
  List(
    useSuperShell := false,
    scalaVersion := scala212,
    scalacOptions += "-Yrangepos",
    organization := "com.geirsson",
    version ~= { old => old.replace('+', '-') },
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/olafurpg/metaconfig")),
    autoAPIMappings := true,
    apiURL := Some(url("https://github.com/olafurpg/metaconfig")),
    developers += Developer(
      "olafurpg",
      "Ólafur Páll Geirsson",
      "olafurpg@gmail.com",
      url("https://geirsson.com")
    ),
    scalaVersion := ScalaVersions.head,
    crossScalaVersions := ScalaVersions,
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
)

addCommandAlias(
  "scalafixAll",
  s"; ++$scala212 ; scalafixEnable ; all scalafix test:scalafix"
)
addCommandAlias(
  "scalafixCheckAll",
  s"; ++$scala212 ;  scalafixEnable ; scalafix --check ; test:scalafix --check"
)

addCommandAlias(
  "native-image",
  "; tests/graalvm-native-image:packageBin ; taskready"
)
commands += Command.command("taskready") { s =>
  import scala.sys.process._
  "afplay /System/Library/Sounds/Hero.aiff".!
  s
}

lazy val warnUnusedImport = Def.setting {
  if (scalaVersion.value.startsWith("2.13")) "-Wunused:imports"
  else "-Ywarn-unused-import"
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

lazy val sharedSettings = List[Setting[_]](
  scalacOptions ++= List(
    "-Yrangepos",
    "-deprecation",
    warnUnusedImport.value
  ),
  mimaBinaryIssueFilters ++= List[ProblemFilter](
    languageAgnosticCompatibilityPolicy
  ),
  mimaPreviousArtifacts := Set("com.geirsson" %% moduleName.value % "0.9.10")
)

skip.in(publish) := true
disablePlugins(MimaPlugin)

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-core"))
  .settings(
    sharedSettings,
    moduleName := "metaconfig-core",
    libraryDependencies ++= List(
      "org.typelevel" %%% "paiges-core" % "0.4.1",
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.4.3",
      scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided,
      "com.lihaoyi" %%% "pprint" % "0.6.4"
    )
  )
lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native

lazy val json = project
  .in(file("metaconfig-json"))
  .settings(
    sharedSettings,
    moduleName := "metaconfig-json",
    libraryDependencies ++= List(
      "com.lihaoyi" %%% "upickle" % "1.3.11"
    )
  )
  .dependsOn(coreJVM)

lazy val typesafe = project
  .in(file("metaconfig-typesafe-config"))
  .settings(
    sharedSettings,
    moduleName := "metaconfig-typesafe-config",
    description := "Integration for HOCON using typesafehub/config.",
    libraryDependencies += "com.typesafe" % "config" % "1.4.1"
  )
  .dependsOn(coreJVM)

lazy val sconfig = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-sconfig"))
  .settings(
    sharedSettings,
    moduleName := "metaconfig-sconfig",
    description := "Integration for HOCON using ekrich/sconfig.",
    libraryDependencies ++= List(
      "org.ekrich" %%% "sconfig" % "1.4.2"
    )
  )
  .jsSettings(
    libraryDependencies ++= List(
      "org.ekrich" %%% "sjavatime" % "1.1.3"
    )
  )
  .nativeSettings(
    libraryDependencies ++= List(
      "org.ekrich" %%% "sjavatime" % "1.1.3"
    )
  )
  .dependsOn(core)
lazy val sconfigJVM = sconfig.jvm
lazy val sconfigJS = sconfig.js
lazy val sconfigNative = sconfig.native

lazy val tests = crossProject(JVMPlatform, JSPlatform)
  .in(file("metaconfig-tests"))
  .disablePlugins(MimaPlugin)
  .settings(
    sharedSettings,
    skip in publish := true,
    publishArtifact.in(Compile, packageDoc) := false,
    testFrameworks := List(new TestFramework("munit.Framework")),
    libraryDependencies ++= List(
      "org.scalameta" %%% "munit-scalacheck" % V.munit,
      "com.github.alexarchambault" %%% "scalacheck-shapeless_1.14" % "1.2.5" // no native
    )
  )
  .jsSettings(
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .jvmSettings(
    mainClass in GraalVMNativeImage := Some("metaconfig.tests.ExampleMain"),
    sources.in(Compile, doc) := Seq.empty,
    graalVMNativeImageOptions ++= {
      val reflectionFile =
        Keys.sourceDirectory.in(Compile).value./("graal")./("reflection.json")
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
        "-H:+ReportExceptionStackTraces"
      )
    }
  )
  .jvmConfigure(
    _.enablePlugins(GraalVMNativeImagePlugin)
      .dependsOn(json, typesafe, sconfigJVM, docs)
  )
  .dependsOn(core)
lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js

lazy val docs = project
  .in(file("metaconfig-docs"))
  .settings(
    sharedSettings,
    moduleName := "metaconfig-docs",
    libraryDependencies ++= List(
      "com.lihaoyi" %% "scalatags" % "0.9.4"
    ),
    mdocVariables := Map(
      "VERSION" -> version.value.replaceFirst("\\+.*", ""),
      "SCALA_VERSION" -> scalaVersion.value
    ),
    mdocOut :=
      baseDirectory.in(ThisBuild).value / "website" / "target" / "docs",
    mdocExtraArguments := List("--no-link-hygiene")
  )
  .dependsOn(coreJVM, json, typesafe, sconfigJVM)
  .enablePlugins(DocusaurusPlugin)
  .disablePlugins(MimaPlugin)
